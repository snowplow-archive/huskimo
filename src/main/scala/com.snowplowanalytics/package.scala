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
package com.snowplowanalytics

// Joda-Time
import org.joda.time.DateTime

// Scalaz
import scalaz._
import Scalaz._

// This project
import huskimo.AppConfig
import huskimo.channels.singular.CampaignStatistics

package object huskimo {

  /**
   * Alias for a Validation containing either
   * a Failure String or a Success-boxed
   * Config.
   */
  type ValidatedString = Validation[String, String]

  /**
   * Alias for a Validation containing either
   * a Failure String or a Success-boxed
   * Config.
   */
  type ValidatedConfig = Validation[String, AppConfig.Config]

  /**
   * Alias for a Validation containing either
   * a Failure String or a Succes-boxed
   * Joda DateTime.
   */
  type ValidatedDateTime = Validation[String, DateTime]

  /**
   * Alias for a Validation containing either
   * a Failure String or a List (possibly empty)
   * of CampaignStatistics on Success.
   */
  type ValidatedCampaignStatistics = Validation[String, List[CampaignStatistics]]
}
