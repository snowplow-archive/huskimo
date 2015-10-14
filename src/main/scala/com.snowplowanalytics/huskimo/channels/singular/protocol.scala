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
package singular

// Joda-Time
import org.joda.time.DateTime

// json4s
import org.json4s.{
  DefaultFormats,
  Formats
}

// Scalaz
import scalaz._
import Scalaz._

// Spray
import spray.httpx.Json4sSupport

// This project
import tasks.{RedshiftTasks => RT}
import utils.{ConversionUtils => CU, DateTimeFormatters => DTF}

/**
 * Marshalling/unmarshalling for the Singular API's JSONs.
 */
object ApiProtocol extends Json4sSupport {

  override implicit def json4sFormats: Formats = DefaultFormats

  type CampaignStatisticsResult = List[CampaignStatistics]

  type CreativeStatisticsResult = List[CreativeStatistics]

  case class ErrorResult(
    error: String
  )
}

case class ApiError(error: String)

case class CampaignStatistics(
  ad_network:          String,
  campaign_name:       String,
  campaign_type:       String,
  campaign_url:        String,
  subcampaign_name:    String,
  app_id:              String,
  campaign_network_id: Option[String],
  country:             String,
  date:                String, // Will be converted before loading into Redshift
  impressions:         Option[Integer],
  clicks:              Option[Integer],
  installs:            Option[Integer],
  cost:                Option[Double],
  revenue:             Option[Double],
  last_modified:       String // Will be converted before loading into Redshift
  ) extends Tsvable {

  /**
   * A helper to convert a CampaignStatistics to a
   * List of tab-separated values, ready
   * for COPYing into Redshift.
   *
   * @param channelName The business name for this channel
   * @param whenRetrieved When the retrieval was performed
   *
   * @return a Validation boxing all fields in the entity as a well-ordered
   *         List of tab-separated value Strings on Success, or a NEL of
   *         error Strings on Failure
   */
  def toTsv(channelName: String, whenRetrieved: DateTime): ValidationNel[String, Array[String]] = {

    val reportingDate = CU.rawToRedshift(date, DTF.YyyyMmDd)
    val lastModified = CU.rawToRedshift(last_modified, DTF.Iso8601)

    (reportingDate.toValidationNel |@| lastModified.toValidationNel) { (rd, lm) =>
      Array(
        channelName,
        CU.dateTimeToRedshift(whenRetrieved),
        CU.tsvify(ad_network),
        CU.tsvify(campaign_name),
        CU.tsvify(campaign_type),
        CU.tsvify(campaign_url),
        CU.tsvify(subcampaign_name),
        CU.tsvify(app_id),
        CU.tsvify(campaign_network_id),
        CU.tsvify(country),
        rd,
        CU.tsvify(impressions),
        CU.tsvify(clicks),
        CU.tsvify(installs),
        CU.tsvify(cost),
        CU.tsvify(revenue),
        lm)
    }
  }
}

case class CreativeStatistics(
  ad_network:          String,
  creative_name:       String,
  creative_text:       String,
  creative_url:        String,
  creative_network_id: String,
  image:               String,
  image_hash:          String,
  width:               Option[Integer],
  height:              Option[Integer],
  is_video:            Boolean,
  campaign_name:       String,
  campaign_type:       String,
  campaign_url:        String,
  subcampaign_name:    String,
  app_id:              String,
  campaign_network_id: Option[String],
  date:                String, // Will be converted before loading into Redshift
  impressions:         Option[Integer],
  clicks:              Option[Integer],
  installs:            Option[Integer],
  cost:                Option[Double],
  modified_at:         String // Will be converted before loading into Redshift
  ) extends Tsvable {

  /**
   * A helper to convert a CampaignStatistics to a
   * List of tab-separated values, ready
   * for COPYing into Redshift.
   *
   * @param channelName The business name for this channel
   * @param whenRetrieved When the retrieval was performed
   *
   * @return a Validation boxing all fields in the entity as a well-ordered
   *         List of tab-separated value Strings on Success, or a NEL of
   *         error Strings on Failure
   */
  def toTsv(channelName: String, whenRetrieved: DateTime): ValidationNel[String, Array[String]] = {

    val reportingDate = CU.rawToRedshift(date, DTF.YyyyMmDd)
    val modifiedAt = CU.rawToRedshift(modified_at, DTF.Iso8601)

    (reportingDate.toValidationNel |@| modifiedAt.toValidationNel) { (rd, ma) =>
      Array(
        channelName,
        CU.dateTimeToRedshift(whenRetrieved),
        CU.tsvify(ad_network),
        CU.tsvify(creative_name),
        CU.tsvify(creative_text),
        CU.tsvify(creative_url),
        CU.tsvify(creative_network_id),
        CU.tsvify(image),
        CU.tsvify(image_hash),
        CU.tsvify(width),
        CU.tsvify(height),
        CU.tsvify(is_video),
        CU.tsvify(campaign_name),
        CU.tsvify(campaign_type),
        CU.tsvify(campaign_url),
        CU.tsvify(subcampaign_name),
        CU.tsvify(app_id),
        CU.tsvify(campaign_network_id),
        rd,
        CU.tsvify(impressions),
        CU.tsvify(clicks),
        CU.tsvify(installs),
        CU.tsvify(cost),
        ma)
    }
  }
}
