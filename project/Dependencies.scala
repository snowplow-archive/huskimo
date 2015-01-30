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
import sbt._

object Dependencies {

  val resolutionRepos = Seq(
    "TypesafeMaven" at "http://repo.typesafe.com/typesafe/maven-releases",
    "Typesafe" at "http://repo.typesafe.com/typesafe/releases",
    "Spray nightlies" at "http://nightlies.spray.io/",
    "Spray repo" at "http://repo.spray.io",
    "oss" at "https://oss.sonatype.org/",
    "Snowplow" at "http://maven.snplow.com/releases/"
  )

  object V {
    // Java
    val snakeyaml        = "1.14"
    val postgres         = "8.4-702.jdbc4"
    val opencsv          = "2.3"
    val yodaTime         = "2.1"
    val aws              = "1.8.9.1"
    val commonsCodec     = "1.5"
    // Scala
    val argot            = "1.0.1"
    val scalaz7          = "7.0.0"
    val json4s           = "3.2.11"
    val scalikeJdbc      = "2.2.0"
    val akka             = "2.3.5"
    val sprayClient      = "1.3.2"
    // Scala (test only)
    val specs2           = "1.14"
  }

  object Libraries {
    // Java
    val snakeyaml        = "org.yaml"                   %  "snakeyaml"                 % V.snakeyaml
    val postgres         = "postgresql"                 %  "postgresql"                % V.postgres
    val opencsv          = "net.sf.opencsv"             %  "opencsv"                   % V.opencsv
    val aws              = "com.amazonaws"              %  "aws-java-sdk"              % V.aws
    val yodaTime         = "joda-time"                  %  "joda-time"                 % V.yodaTime
    val commonsCodec     = "commons-codec"              %  "commons-codec"             % V.commonsCodec
    // Scala
    val argot            = "org.clapper"                %% "argot"                     % V.argot
    val scalaz7          = "org.scalaz"                 %% "scalaz-core"               % V.scalaz7
    val json4sNative     = "org.json4s"                 %% "json4s-native"             % V.json4s
    val scalikeJdbc      = "org.scalikejdbc"            %% "scalikejdbc"               % V.scalikeJdbc
    val akka             = "com.typesafe.akka"          %% "akka-actor"                % V.akka
    val sprayClient      = "io.spray"                   %% "spray-client"              % V.sprayClient
    // Scala (test only)
    val specs2           = "org.specs2"                 %% "specs2"                    % V.specs2         % "test"
    val scalikeJdbcTest  = "org.scalikejdbc"            %% "scalikejdbc-test"          % V.scalikeJdbc    % "test"
    val akkaTestkit      = "com.typesafe.akka"          %% "akka-testkit"              % V.akka           % "test"
  }
}
