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

// AWS
import com.amazonaws.services.s3.AmazonS3Client

// Scalaz
import scalaz._
import Scalaz._

// Spray
import spray.httpx.unmarshalling.FromResponseUnmarshaller

// This project
import ApiProtocol._
import tasks.{
  FileTasks,
  RedshiftTasks
}
import utils.ConversionUtils

object Singular {

  private case class Resource(apiSlug: String, filePrefix: String, tableName: String)

  private object Resources {
    private val tablePrefix = "huskimo.singular_"
    val campaigns = Resource("stats", "campaigns", s"${tablePrefix}campaigns")
    val creatives = Resource("creative_stats", "creatives", s"${tablePrefix}creatives")
  }

  /**
   * Fetch from API and write to file.
   *
   * @param config The Config for the HuskimoApp
   * @param channel The Channel we are fetching from
   * @param resource The resource we are fetching
   * @param lookupDate The date we are fetching
   */
  def fetchAndWrite[T <: List[Tsvable] : FromResponseUnmarshaller](config: AppConfig.Config,
    channel: AppConfig.Channel, channelIndex: Int, resource: Resource,
    lookupDate: DateTime)(implicit s3: AmazonS3Client) {

    ApiClient.getStatistics[T](channel.api_key, resource.apiSlug, lookupDate) match {
      case Success(records) => {
        val filename = FileTasks.getTemporaryFile(channelIndex, resource.filePrefix, lookupDate)
        FileTasks.writeFile(filename, records, channel.name, ConversionUtils.now())
        FileTasks.uploadToS3(s3, config.s3.bucket, config.s3.folder_path, filename)
      }
      case Failure(err) => throw new Exception(s"Error fetching campaigns from Singular (${channel.name}): ${err}") // TODO: send event to Snowplow & non-0 system exit
    } 
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
    FileTasks.deleteFromS3(s3Client, config.s3.bucket, Left(config.s3.folder_path))

    // 2. Pagination
    // TODO: this should be in parallel
    val singularChannels = config.channels.filter(_.`type` == "singular")
    for ((chn, idx) <- singularChannels.zipWithIndex) {
      // Loop through all days
      for (daysAgo <- 0 to config.fetch.lookback) {
        val lookupDate = endDate.minusDays(daysAgo)
        // Lookup the resources and write to a temporary file
        fetchAndWrite[CampaignStatisticsResult](config, chn, idx, Resources.campaigns, lookupDate)
        fetchAndWrite[CreativeStatisticsResult](config, chn, idx, Resources.creatives, lookupDate)
      }

      // TODO: this should be in parallel
      for (tgt <- config.targets) {
        RedshiftTasks.initializeConnection(tgt)
        RedshiftTasks.loadTable(config.s3, Resources.campaigns.filePrefix, Resources.campaigns.tableName)
        RedshiftTasks.loadTable(config.s3, Resources.creatives.filePrefix, Resources.creatives.tableName)
      }
    }
    ApiClient.shutdown()
  }

}