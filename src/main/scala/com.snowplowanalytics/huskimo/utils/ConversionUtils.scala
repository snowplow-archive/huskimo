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
package utils

// Java
import java.util.Date

// Joda-Time
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

// Scalaz
import scalaz._
import Scalaz._

/**
 * Holds utilities for working with encryption.
 * Adapted from:
 *
 * https://github.com/nefilim/ScalaChefClient/blob/master/src/main/scala/org/nefilim/chefclient/ChefCryptUtil.scala
 */
object ConversionUtils {

  /**
   * Parse the supplied date. Should be in the format
   * YYYY-MM-DD.
   *
   * @param enddate
   * @return a validation containing either a Failure
   *         String or a Success-boxed JodaTime
   */
  def toDateTime(raw: String, dtf: DateTimeFormatter): ValidatedDateTime =
  	try {
  		DateTime.parse(raw, dtf).success
  	} catch {
      case iae: IllegalArgumentException => s"Cannot convert ${raw} to DateTime using ${dtf}".fail
    }

  /**
   * Returns now as a Joda DateTime.
   *
   * @return the date-time now
   */
  def now(): DateTime = DateTime.now(DateTimeZone.UTC)

  /**
   * Makes any type TSV-safe. Be careful with this.
   */
  def tsvify(value: Any): String = tsvify(Option(value))

  /**
   * Makes any Option-boxed type TSV-safe. Be careful with this.
   */
  def tsvify(value: Option[Any]): String = (for {
    raw <- value
    fix <- fixTabsNewlines(raw.toString)
  } yield fix).getOrElse("")

  /**
   * Replaces tabs with four spaces and removes
   * newlines altogether.
   *
   * Useful to prepare user-created strings for
   * fragile storage formats like TSV.
   *
   * @param str The String to fix

   * @return The String with tabs and newlines fixed.
   */
  private def fixTabsNewlines(str: String): Option[String] = {
    val f = for {
      s <- Option(str)
      r = s.replaceAll("\\t", "    ")
           .replaceAll("[\n\r]", " ")
           .replaceAll("\\p{Cntrl}", "") // Any other control character
    } yield r
    if (f == Some("")) None else f
  }

  /**
   * Converts a date to Redshift format.
   */
  def dateToRedshift(dt: Date): String =
    dateTimeToRedshift(new DateTime(dt))

  /**
   * Converts a dateTime to Redshift format.
   */
  def dateTimeToRedshift(dt: DateTime): String =
    DateTimeFormatters.Redshift.print(dt)

  /**
   * Converts an ISO8601 datetime String to a Redshift
   * datetime String.
   */
  def rawToRedshift(raw: String, dtf: DateTimeFormatter): ValidatedString = {
    for {
      dt <- toDateTime(raw, dtf)
    } yield dateTimeToRedshift(dt)
  }
}
