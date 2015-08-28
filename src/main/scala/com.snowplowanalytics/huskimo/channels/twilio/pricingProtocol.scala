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

// json4s
import org.json4s.{
  DefaultFormats,
  Formats
}

// Scalaz
import scalaz._
import Scalaz._

// Spray
import spray.httpx.Json4sSupport

// This project
import tasks.{RedshiftTasks => RT}
import utils.{ConversionUtils => CU, DateTimeFormatters => DTF}

/**
 * Marshalling/unmarshalling for the Singular API's JSONs.
 */
object PricingApiProtocol extends Json4sSupport {

  override implicit def json4sFormats: Formats = DefaultFormats

  /* {
     "uri": "pricing.twilio.com/v1/PhoneNumbers/Countries",
     "countries": [
         {
             "country": "Ascension",
             "iso_country": "AC",
             "uri": "pricing.twilio.com/v1/PhoneNumbers/Countries/AC"
         },
         {
             "country": "Andorra",
             "iso_country": "AD",
             "uri": "pricing.twilio.com/v1/PhoneNumbers/Countries/AD"
         },
  ...
     ]
  } */
  case class CountriesResult(countries: List[Country])

  /* {
     "uri": "pricing.twilio.com/v1/PhoneNumbers/Countries/EE"
     "country” : "Estonia"
     "iso_country": "EE",
     "phone_number_prices": [
         {
           "number_type": "mobile",
           "base_price": "3.00",
           "current_price": "3.00"
         },
         {
           "number_type": "national",
           "base_price": "1.00",
           "current_price": "1.00"
         }
     ],
     "price_unit": "usd"
  } */
  case class PhoneNumbersResult(
    url: String,
    country: String,
    iso_country: String,
    phone_number_prices: List[PhoneNumberPrice],
    price_unit: String)

  /* {
     "code": 20003,
     "detail": "Your AccountSid or AuthToken was incorrect.",
     "message": "Authentication Error - No credentials provided",
     "more_info": "https://www.twilio.com/docs/errors/20003",
     "status": 401
  } */
  case class ErrorResult(
    code: Int,
    detail: String,
    message: String,
    more_info: String,
    status: Int
  )
}

case class Country(country: String, iso_country: String, url: String)

case class PhoneNumberPrice(number_type: String, base_price: String, current_price: String)

case class PricingPhoneNumber(
  country: String,
  isoCountry: String,
  priceUnit: String,
  numberType: String,
  basePrice: String,
  currentPrice: String
  ) extends Tsvable {

  /**
   * A helper to convert a PricingPhoneNumber to a
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
      CU.tsvify(country),
      CU.tsvify(isoCountry),
      CU.tsvify(priceUnit),
      CU.tsvify(numberType),
      CU.tsvify(basePrice),
      CU.tsvify(currentPrice)).successNel[String]

}
