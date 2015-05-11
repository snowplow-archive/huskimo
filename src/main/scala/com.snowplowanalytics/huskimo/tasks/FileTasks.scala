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

// Java
import java.io.{
  File,
  FileOutputStream,
  OutputStreamWriter
}
import java.util.zip.GZIPOutputStream

// Joda-Time
import org.joda.time.DateTime

// OpenCSV
import au.com.bytecode.opencsv.CSVWriter

// AWS
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.ObjectListing

// Scala
import scala.collection.JavaConversions._
import scala.annotation.tailrec

// Scalaz
import scalaz._
import Scalaz._

// This project
import channels.Tsvable
import utils.{DateTimeFormatters => DTF}

/**
 * Contains functions to help with the file-oriented
 * parts of the process. Includes S3-related tasks.
 */
object FileTasks {

  private object TempFile {
    val suffix = ".tsv.gz"
  }

  private val BlockSize = 16384

  /**
   * Initialize our S3 client from the AWS SDK
   *
   * @param accessKeyId The AWS access key ID
   * @param secretAccessKey The AWS secret access key
   * @return our initialized S3 client
   */
  def initializeS3Client(accessKeyId: String, secretAccessKey: String): AmazonS3Client = {
    val creds = new BasicAWSCredentials(accessKeyId, secretAccessKey)
    new AmazonS3Client(creds)
  }

  /**
   * Makes sure there are no files at the S3 bucket
   * path we are going to upload our files to.
   *
   * @param s3Client Our S3 client from the AWS SDK
   * @param bucket The bucket to delete from
   * @param target Either the folder path to start
   *        deleting from, or the next ObjectListing
   *        to delete
   */
  @tailrec
  def deleteFromS3(s3Client: AmazonS3Client, bucket: String, target: Either[String, ObjectListing]) {
    val listing = target match {
      case Left(folderPath) => s3Client.listObjects(bucket, folderPath)
      case Right(nextBatch) => nextBatch
    }
    for (file <- listing.getObjectSummaries) {
      s3Client.deleteObject(bucket, file.getKey)
    }
    if (listing.isTruncated) {
      deleteFromS3(s3Client, bucket, Right(s3Client.listNextBatchOfObjects(listing)))
    }
  }

  /**
   * Generate the temporary file path to hold
   * our TSV-format campaign data.
   *
   * @param channelIndex To prevent clashes
   *        between separate marketing channels
   * @param resource What type of resource are
   *        we storing in our file
   * @param date A timestamp for our filename
   * 
   * @return the temporary file
   */
  def getTemporaryFile(channelIndex: Int, resource: String, date: DateTime): File = {
    val dt = DTF.YyyyMmDdCompact.print(date)
    val temp = File.createTempFile(s"${resource}-${channelIndex}-${dt}-", TempFile.suffix)
    System.out.println(s"Temporary file is ${temp}")
    temp.deleteOnExit
    temp.getCanonicalFile
  }

  /**
   * Writes out all campaign data in TSV-format
   * to a file.
   *
   * @param file The temporary file to write the
   *        the events to.
   * @param records (Possibly empty) List of
   *        records which can be converted to a TSV.
   * @param channelName The business name for this channel
   * @param whenRetrieved When the retrieval was performed
   */
  def writeFile(file: File, records: List[Tsvable],
    channelName: String, whenRetrieved: DateTime) {

    val osw = {
      val fis  = new FileOutputStream(file)
      val gzos = new GZIPOutputStream(fis, BlockSize)
      new OutputStreamWriter(gzos)
    }

    val writer = new CSVWriter(osw, AppConfig.FieldDelimiter.AsChar, CSVWriter.NO_QUOTE_CHARACTER)
    for (event <- records) {
      // TODO: send all errors to a stream instead of panicking
      event.toTsv(channelName, whenRetrieved) match {
        case Failure(nel) => throw new Exception("Error(s) converting record to TSV: \n" + nel.toList.mkString("\n"))
        case Success(tsv) => writer.writeNext(tsv)
      }
    }
    writer.close()
  }

  /**
   * Uploads the file of campaign data to Amazon
   * S3.
   *
   * @param config The configuration for Amazon S3
   * @param bucket The bucket to delete from
   * @param folderPath The path to delete from
   * @param file The file to upload
   */
  def uploadToS3(s3Client: AmazonS3Client, bucket: String, folderPath: String, file: File) {
    val key = s"${folderPath}/${file.getName}"
    s3Client.putObject(new PutObjectRequest(bucket, key, file))
  }

}