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

// Scala
import scala.reflect.BeanProperty

/**
 * Contains the configuration classes for this app.
 * Structured as POJOs with no-args constructors to
 * allow SnakeYAML to work with them.
 */
object AppConfig {

  object FieldDelimiter {
    val AsChar = 1.asInstanceOf[Char]
    val AsString = "\\001"
  }

  class Fetch {
    @BeanProperty var lookback: Int = _
  }

  class Channel {
    @BeanProperty var name:    String = _
    @BeanProperty var `type`:  String = _
    @BeanProperty var api_user: String = _
    @BeanProperty var api_key: String = _
  }

  class S3 {
    @BeanProperty var access_key_id:     String = _
    @BeanProperty var secret_access_key: String = _
    @BeanProperty var region:            String = _
    @BeanProperty var bucket:            String = _
    @BeanProperty var folder_path:       String = _
  }

  class Target {
    @BeanProperty var name:        String = _
    @BeanProperty var `type`:      String = _
    @BeanProperty var host:        String = _
    @BeanProperty var database:    String = _
    @BeanProperty var port:        Int = _
    @BeanProperty var ssl:         Boolean = _
    @BeanProperty var ssl_factory: String = _
    @BeanProperty var schema:      String = _
    @BeanProperty var username:    String = _
    @BeanProperty var password:    String = _
  }

  class Config {
    @BeanProperty var fetch:    Fetch = _
    @BeanProperty var s3:       S3 = _
    @BeanProperty var channels: Array[Channel] = _
    @BeanProperty var targets:  Array[Target] = _
  }
}
