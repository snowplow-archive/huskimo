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

// Java
import java.io.{
  File,
  IOException,
  FileInputStream
}

// Logging
import org.slf4j.LoggerFactory

// Joda-Time
import org.joda.time.DateTime

// SnakeYAML
import org.yaml.snakeyaml.{
  Yaml,
  TypeDescription
}
import org.yaml.snakeyaml.constructor.Constructor

// Scalaz
import scalaz._
import Scalaz._

// Argot
import org.clapper.argot._

// This project
import utils.{
  ConversionUtils,
  DateTimeFormatters
}
import channels.singular.Singular
import channels.twilio.Twilio

/**
 * Main entry point of the Huskimo app.
 */
object HuskimoApp extends App {
  lazy val log = LoggerFactory.getLogger(getClass())
  import log.{error, debug, info, trace}

  import ArgotConverters._ // Argument specifications

  val parser = new ArgotParser(
    programName = generated.ProjectSettings.name,
    compactUsage = true,
    preUsage = Some("%s: Version %s. Copyright (c) 2014, %s.".format(
      generated.ProjectSettings.name,
      generated.ProjectSettings.version,
      generated.ProjectSettings.organization)
    )
  )

  // Mandatory config argument
  import Huskimo._
  val config = parser.option[ValidatedConfig](List("config"), "filename", "Configuration file") { (c, _) =>
    parseYaml(c)
  }
  val endDate = parser.option[ValidatedDateTime](List("e", "enddate"), "n",
                                      "Fetch up to this date (YYYY-MM-DD) inclusive. Defaults to today. UTC") { (ed, _) =>
    ConversionUtils.toDateTime(ed, DateTimeFormatters.YyyyMmDd)
  }
  // TODO: add an optional startdate which overrides the duration set in the config file?
  parser.parse(args)

  (config.value, endDate.value) match {
    case (None, _)               => parser.usage("--config option must be provided")
    case (Some(Failure(err)), _) => parser.usage(err)
    case (_, Some(Failure(err))) => parser.usage(err)
    case (Some(Success(conf)), None)              => fetchAll(conf, ConversionUtils.now())
    case (Some(Success(conf)), Some(Success(dt))) => fetchAll(conf, dt)
  }
}

/**
 * Helpers for the Huskimo app.
 */
object Huskimo {

  /**
   * Parse the YAML configuration file.
   *
   * @param path The path to the YAML file
   * @return a Validation containing either
   *         a Failure String or a Success-boxed
   *         Config.
   */
  def parseYaml(path: String): ValidatedConfig = {

    val file = new File(path)
    if (file.exists) {
      val input = new FileInputStream(file)
      
      import AppConfig._
      val yaml = {
        val c = new Constructor(classOf[Config])
        for (klazz <- List(classOf[Channel], classOf[S3], classOf[Target])) {
          c.addTypeDescription(new TypeDescription(klazz))
        }
        new Yaml(c)
      }

      try {
        yaml.load(input).asInstanceOf[Config].success
      } catch {
        case e: Exception => (s"""Error reading configuration file "${path}": """ + e.getMessage).fail
      }
    } else {
      s"""Configuration file "${path}" does not exist""".fail
    }
  }

  /**
   * Fetch from all supported vendors.
   *
   * @param config The Config for the HuskimoApp
   * @param endDate The last day to retrieve
   *        campaign statistics for
   */
  def fetchAll(config: AppConfig.Config, endDate: DateTime) {
    Singular.fetch(config, endDate)
    Twilio.fetch(config, endDate)
  }

}
