/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.huskimo
package channels
package twilio

// Java
import java.lang.{Iterable => JIterable}
import java.util.{HashMap => JHashMap}

// Joda-Time
import org.joda.time.{
  DateTime,
  DateTimeZone
}

// AWS
import com.amazonaws.services.s3.AmazonS3Client

// Twilio
import com.twilio.sdk.TwilioRestClient
import com.twilio.sdk.resource.InstanceResource
import com.twilio.sdk.resource.instance.{
  Call,
  IncomingPhoneNumber,
  Message,
  Recording
}

// Scala
import scala.collection.JavaConverters._
import scala.annotation.tailrec

// Scalaz
import scalaz._
import Scalaz._

// Spray
import spray.httpx.unmarshalling.FromResponseUnmarshaller

// This project
import PricingApiProtocol._
import tasks.{
  FileTasks,
  RedshiftTasks
}
import utils.{
  ConversionUtils,
  DateTimeFormatters
}

object Twilio {

  private val PageSize = 1000

  private val TwilioTimeZone = DateTimeZone.UTC

  private object Resources {

    // TODO: we could back calculate dateColumn from dateProperty
    sealed abstract class Resource(val name: String, val dateProperty: Option[String], val dateColumn: Option[String]) {
      val tableName = s"huskimo.twilio_${name}"
    }

    case object Calls extends Resource("calls", "StartTime".some, "start_time".some)
    case object IncomingPhoneNumbers extends Resource("incoming_phone_numbers", None, None)
    case object Messages extends Resource("messages", "DateSent".some, "date_sent".some)
    case object Recordings extends Resource("recordings", "DateCreated".some, "date_created".some)
    case object PricingPhoneNumbers extends Resource("pricing_phone_numbers", None, None)
  }

  private case class DateRange(start: Option[DateTime], end: DateTime)

  /**
   * Fetch from API and write to file.
   *
   * @param config The Config for the HuskimoApp
   * @param channel The Channel we are fetching from
   * @param channelIndex The index of the Channel we are fetching from
   * @param resource The resource we are fetching
   * @param lookupRange The dates we are looking up
   */
  private def fetchAndWrite(config: AppConfig.Config,
    channel: AppConfig.Channel, channelIndex: Int, resource: Resources.Resource,
    lookupRange: Option[DateRange])
    (implicit twilio: TwilioRestClient, s3: AmazonS3Client) {

    def writeToS3[A <: InstanceResource[TwilioRestClient]](iterable: JIterable[A]) {

      val iterator = iterable.asScala.toStream.sliding(PageSize, PageSize)
      for ((page, index) <- iterator.zipWithIndex) {
        val tsvablePage = page.toList.map(Tsvable.convert(_))
        val filename = FileTasks.getTemporaryFile(channelIndex, resource.name, index.toString)
        FileTasks.writeFile(filename, tsvablePage, channel.name, ConversionUtils.now())
        FileTasks.uploadToS3(s3, config.s3.bucket, config.s3.folder_path, filename)
      }
    }

    val acc = twilio.getAccount()
    val params = getParams(resource.dateProperty, lookupRange)
    resource match {
      case Resources.Calls                => writeToS3[Call](acc.getCalls(params))
      case Resources.IncomingPhoneNumbers => writeToS3[IncomingPhoneNumber](acc.getIncomingPhoneNumbers(params))
      case Resources.Messages             => writeToS3[Message](acc.getMessages(params))
      case Resources.Recordings           => writeToS3[Recording](acc.getRecordings(params))
      case _                              => throw new IllegalArgumentException("Unsupported resource type")
    }
  }

  /**
   * Fetch from the Pricing API and write to file.
   *
   * @param config The Config for the HuskimoApp
   * @param channel The Channel we are fetching from
   * @param channelIndex The index of the Channel we are fetching from
   * @param resource The resource we are fetching
   */
  def fetchAndWritePricing(config: AppConfig.Config,
    channel: AppConfig.Channel, channelIndex: Int, resource: Resources.Resource)
    (implicit s3: AmazonS3Client) {

      val countries = PricingApiClient.getPhoneNumbers[CountriesResult](channel.api_user, channel.api_key, None)
      countries match {
        case Success(countries) => {

          val numbers: List[PricingPhoneNumber] = for {
            c <- countries.countries
            pricing = PricingApiClient.getPhoneNumbers[PhoneNumbersResult](
              channel.api_user, channel.api_key, Some(c.iso_country)).toOption.get // TODO: we should collect errors
            price <- pricing.phone_number_prices
          } yield PricingPhoneNumber(c.country, c.iso_country, pricing.price_unit, price.number_type, price.base_price, price.current_price)

          val filename = FileTasks.getTemporaryFile(channelIndex, resource.name, "0")
          FileTasks.writeFile(filename, numbers, channel.name, ConversionUtils.now())
          FileTasks.uploadToS3(s3, config.s3.bucket, config.s3.folder_path, filename)
        }
        case Failure(failure) => throw new RuntimeException(s"Couldn't get list of countries from pricing API: ${failure}")
      }
  }

  /**
   * Convert a maybe TimeRange into a complete
   * set of parameters for the get.
   *
   * @param dateProperty The resource property
   *        to apply the lookup date range to
   * @param lookupRange The dates we are looking up
   * @return a Java HashMap of parameters in a
   *         Twilio-compatible format
   */
  private def getParams(dateProperty: Option[String], lookupRange: Option[DateRange]): JHashMap[String, String] = {
    val params = new JHashMap[String, String]()
    (dateProperty, lookupRange) match {
      case (Some(dp), Some(DateRange(maybeStart, end))) => {
        // TODO: replace mutation of HashMap with something more declarative
        params.put(s"${dp}<", DateTimeFormatters.YyyyMmDd.print(end))
        for (start <- maybeStart) {
          params.put(s"${dp}>", DateTimeFormatters.YyyyMmDd.print(start))
        }
        params
      }
      case _ => params
    }
  }

  /**
   * Get the lookup date range. Should range
   * from: the last date found in Redshift + 1 day
   * to: yesterday
   *
   * @param table Table name (optionally starting
   *        with schema) containing the timestamp
   * @param column Name of column containing the
   *        timestamp
   */
  private def getLookupRange(table: String, column: String): DateRange = {

    val max = RedshiftTasks.getMaxTimestamp(table, column)
    val from = max.map(_.plusDays(1)) // Because Twilio > is greater than or equal to ("from midnight on the day")
    val today = new DateTime(TwilioTimeZone) // Because Twilio < is less than ("up to midnight on the day")

    DateRange(from, today)
  }

  /**
   * Run the fetch process for all Singular resources
   * that we care about.
   *
   * @param config The Config for the HuskimoApp
   * @param endDate The last day to retrieve
   *        campaign statistics for
   */
  def fetch(config: AppConfig.Config, endDate: DateTime) {

    // 1. Setup
    // TODO: initialize for each database
    implicit val s3Client = FileTasks.initializeS3Client(config.s3.access_key_id, config.s3.secret_access_key)
    // TODO: no need to do this delete on a per channel basis
    FileTasks.deleteFromS3(s3Client, config.s3.bucket, Left(config.s3.folder_path))

    // 2. Pagination
    // TODO: this should be in parallel
    val twilioChannels = config.channels.filter(_.`type` == "twilio")
    for ((chn, idx) <- twilioChannels.zipWithIndex) {

      // 2.1 Setup
      implicit val twilioClient = new TwilioRestClient(chn.api_user, chn.api_key)

      // 2.2 Get the last stored dates
      // TODO: this ought really be database specific
      // TODO: the .gets are nasty
      val firstTarget = config.targets(0)
      RedshiftTasks.initializeConnection(firstTarget)
      val callsLookupRange = getLookupRange(Resources.Calls.tableName, Resources.Calls.dateColumn.get)
      val messagesLookupRange = getLookupRange(Resources.Messages.tableName, Resources.Messages.dateColumn.get)
      val recordingsLookupRange = getLookupRange(Resources.Recordings.tableName, Resources.Recordings.dateColumn.get)

      // 2.3 Lookup the resources and write to a temporary file
      // Probably little benefit to doing these in parallel given API rate limiting
      fetchAndWrite(config, chn, idx, Resources.Calls, callsLookupRange.some)
      fetchAndWrite(config, chn, idx, Resources.Messages, messagesLookupRange.some)
      fetchAndWrite(config, chn, idx, Resources.Recordings, recordingsLookupRange.some)
      fetchAndWrite(config, chn, idx, Resources.IncomingPhoneNumbers, None)
      fetchAndWritePricing(config, chn, idx, Resources.PricingPhoneNumbers)

      // 2.4 Copy files into Redshift
      // TODO: this should be in parallel
      for (tgt <- config.targets) {
        RedshiftTasks.initializeConnection(tgt)

        // Upload new
        RedshiftTasks.loadTable(config.s3, Resources.Calls.name, Resources.Calls.tableName)
        RedshiftTasks.loadTable(config.s3, Resources.Messages.name, Resources.Messages.tableName)
        RedshiftTasks.loadTable(config.s3, Resources.Recordings.name, Resources.Recordings.tableName)

        // Upload all (have to truncate first)
        RedshiftTasks.emptyTable(Resources.IncomingPhoneNumbers.tableName)
        RedshiftTasks.loadTable(config.s3, Resources.IncomingPhoneNumbers.name, Resources.IncomingPhoneNumbers.tableName)
        RedshiftTasks.emptyTable(Resources.PricingPhoneNumbers.tableName)
        RedshiftTasks.loadTable(config.s3, Resources.PricingPhoneNumbers.name, Resources.PricingPhoneNumbers.tableName)
      }
    }
    PricingApiClient.shutdown() // Else hangs
  }

}