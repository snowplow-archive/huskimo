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
package twilio

// Joda-Time
import org.joda.time.DateTime

// Twilio
import com.twilio.sdk.TwilioRestClient
import com.twilio.sdk.resource.InstanceResource
import com.twilio.sdk.resource.instance.{
  Call,
  IncomingPhoneNumber,
  Message,
  Recording
}

// Scala
import scala.language.implicitConversions

// Scalaz
import scalaz._
import Scalaz._

// This project
import utils.{ConversionUtils => CU, DateTimeFormatters => DTF}

object Tsvable {

  def convert[A <: InstanceResource[TwilioRestClient]](resource: A): Tsvable = resource match {
    case call: Call                  => TsvableCall(call)
    case number: IncomingPhoneNumber => TsvableIncomingPhoneNumber(number)
    case message: Message            => TsvableMessage(message)
    case recording: Recording        => TsvableRecording(recording)
    case _                           => throw new IllegalArgumentException("Cannot pimp InstanceResource to Tsvable")
  }
}

case class TsvableCall(call: Call) extends Tsvable {

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
  def toTsv(channelName: String, whenRetrieved: DateTime): ValidationNel[String, Array[String]] =
    Array(
      channelName,
      CU.dateTimeToRedshift(whenRetrieved),
      call.getSid,
      call.getAccountSid,
      call.getApiVersion,
      CU.dateToRedshift(call.getDateCreated),
      CU.dateToRedshift(call.getDateUpdated),
      call.getParentCallSid,
      call.getTo,
      call.getFrom,
      call.getPhoneNumberSid,
      call.getStatus,
      CU.dateToRedshift(call.getStartTime),
      CU.dateToRedshift(call.getEndTime),
      call.getDuration,
      call.getPrice,
      call.getDirection,
      call.getAnsweredBy,
      call.getForwardedFrom,
      call.getCallerName).successNel[String]
}

case class TsvableIncomingPhoneNumber(number: IncomingPhoneNumber) extends Tsvable {

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
  def toTsv(channelName: String, whenRetrieved: DateTime): ValidationNel[String, Array[String]] =
    Array(
      channelName,
      CU.dateTimeToRedshift(whenRetrieved),
      number.getSid,
      number.getAccountSid,
      number.getApiVersion,
      CU.dateToRedshift(number.getDateCreated),
      CU.dateToRedshift(number.getDateUpdated),
      number.getFriendlyName,
      number.getPhoneNumber,
      number.getVoiceApplicationSid,
      number.getSmsApplicationSid,
      number.getVoiceUrl,
      number.getVoiceMethod,
      number.getVoiceFallbackUrl,
      number.getVoiceFallbackMethod,
      number.getStatusCallback,
      number.getStatusCallbackMethod,
      CU.tsvify(number.getVoiceCallerIdLookup),
      number.getSmsUrl,
      number.getSmsMethod,
      number.getSmsFallbackUrl,
      number.getSmsFallbackMethod,
      number.getSmsStatusCallback,
      number.getAddressRequirements).successNel[String]
}

case class TsvableMessage(message: Message) extends Tsvable {

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
  def toTsv(channelName: String, whenRetrieved: DateTime): ValidationNel[String, Array[String]] =
    Array(
      channelName,
      CU.dateTimeToRedshift(whenRetrieved),
      message.getSid,
      message.getAccountSid,
      message.getApiVersion,
      CU.dateToRedshift(message.getDateCreated),
      CU.dateToRedshift(message.getDateUpdated),
      CU.dateToRedshift(message.getDateSent),
      message.getTo,
      message.getFrom,
      CU.tsvify(message.getBody.length),
      message.getStatus,
      message.getPrice,
      message.getPriceUnit,
      CU.tsvify(message.getNumMedia),
      CU.tsvify(message.getNumSegments),
      message.getDirection,
      message.getErrorMessage).successNel[String]
}

case class TsvableRecording(recording: Recording) extends Tsvable {

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
  def toTsv(channelName: String, whenRetrieved: DateTime): ValidationNel[String, Array[String]] =
    Array(
      channelName,
      CU.dateTimeToRedshift(whenRetrieved),
      recording.getSid,
      recording.getAccountSid,
      recording.getApiVersion,
      CU.dateToRedshift(recording.getDateCreated),
      CU.dateToRedshift(recording.getDateUpdated),
      recording.getCallSid,
      CU.tsvify(recording.getDuration)).successNel[String]
}
