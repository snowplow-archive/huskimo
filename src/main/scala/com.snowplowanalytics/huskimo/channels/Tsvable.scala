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

// Joda-Time
import org.joda.time.DateTime

// Scalaz
import scalaz._
import Scalaz._

trait Tsvable {

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
	def toTsv(channelName: String, whenRetrieved: DateTime): ValidationNel[String, Array[String]] 
}