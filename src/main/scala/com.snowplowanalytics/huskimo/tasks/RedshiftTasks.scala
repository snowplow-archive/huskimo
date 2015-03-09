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
package tasks

// ScalikeJDBC
import scalikejdbc._

/**
 * Contains the Redshift-related tasks required
 * for loading the campaign statistics.
 */
object RedshiftTasks {

  private val RedshiftDriver = "org.postgresql.Driver"

  /**
   * Initialize the database connection.
   *
   * @param config The configuration for the Redshift
   *        database target
   */
  def initializeConnection(config: AppConfig.Target) {
    Class.forName(RedshiftDriver)
    ConnectionPool.singleton(s"jdbc:postgresql://${config.host}:${config.port}/${config.database}",
      config.username, config.password)
  }

  /**
   * Loads the new campaigns into Redshift via a COPY
   * statement.
   *
   * @param config The configuration for Amazon S3
   * @param resource What type of resource are
   *        we storing in our file
   * @param tableName The name of the table (optionally starting
   *        with schema) containing the data to load
   */
  def loadTable(config: AppConfig.S3, resource: String, tableName: String) {
    
    val path = buildS3Path(config.bucket, resource, config.folder_path)
    val creds = buildCredentialsString(config.access_key_id, config.secret_access_key)

    DB autoCommit { implicit session =>
      SQL(s"""|COPY ${tableName} FROM '${path}' 
          |CREDENTIALS AS '${creds}' 
          |GZIP 
          |DELIMITER AS '${AppConfig.FieldDelimiter.AsString}' 
          |MAXERROR AS 1 
          |REGION AS '${config.region}' 
          |EMPTYASNULL 
          |ACCEPTINVCHARS;""".stripMargin.replaceAll("[\n\r]",""))
        .execute
        .apply()
    }
  }

  /**
   * Builds a COPY-compatible S3 path.
   *
   * @param bucket The bucket to delete from
   * @param folderPath The path to delete from
   * @return the path to COPY into S3
   */
  def buildS3Path(bucket: String, resource: String, folderPath: String): String =
    s"s3://${bucket}/${folderPath}/${resource}-"

  /**
   * Builds a COPY-compatible credentials string.
   *
   * @param accessKeyId The AWS access key ID
   * @param secretAccessKey The AWS secret access key
   * @return an AWS credentials string in the format
   *         expected by Redshift's COPY statement
   */
  def buildCredentialsString(accessKeyId: String, secretAccessKey: String): String =
    s"aws_access_key_id=${accessKeyId};aws_secret_access_key=${secretAccessKey}"
}
