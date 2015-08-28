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

// Java
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManager
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

// Apache Commons
import org.apache.commons.codec.binary.Base64

// Scala
import scala.annotation.tailrec
import scala.concurrent.{
  Future,
  Await
}
import scala.concurrent.duration._

// Scalaz
import scalaz._
import Scalaz._

// Akka
import akka.io.IO
import akka.actor.{Props, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout

// json4s
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

// Spray
import spray.http._
import spray.http.HttpHeaders.Authorization
import spray.httpx.{UnsuccessfulResponseException => UrUnsuccessfulResponseException}
import spray.httpx.unmarshalling.FromResponseUnmarshaller
import spray.util._
import spray.client.pipelining._
import spray.can.Http
import spray.can.Http.HostConnectorSetup
import spray.io.ClientSSLEngineProvider

// Config
import com.typesafe.config.ConfigFactory

// This project
import utils.{
  ConversionUtils,
  DateTimeFormatters
}
import PricingApiProtocol._

object UnsuccessfulResponseException {
  def unapply(a: UrUnsuccessfulResponseException): Option[HttpResponse]
    = Some(a.response)
}

/**
 * Contains a client for accessing Twilio's new
 * pricing API.
 */
object PricingApiClient {

  private val TwilioApiHost = "pricing.twilio.com"
  private val TwilioApiPort = 443
  private val TwilioApiVersion = "v1"

  private val Encoding = "UTF-8"

  // We need an ActorSystem to host our application in
  implicit val system = ActorSystem(generated.ProjectSettings.name,
    ConfigFactory.parseString("akka.daemonic=on"))
  import system.dispatcher // execution context for futures
  import system.log // for logging

  // Place a special SSLContext in scope here.
  // It trusts all server certificates.
  private implicit def trustfulSslContext: SSLContext = {

    object BlindFaithX509TrustManager extends X509TrustManager {
      def checkClientTrusted(chain: Array[X509Certificate], authType: String) = ()
      def checkServerTrusted(chain: Array[X509Certificate], authType: String) = ()
      def getAcceptedIssuers = Array[X509Certificate]()
    }

    val context = SSLContext.getInstance("TLS")
    context.init(Array[KeyManager](),
                 Array(BlindFaithX509TrustManager),
                 null)
    context
  }

  // We need this to pull in the SSLContext
  lazy val sslEngineProvider: ClientSSLEngineProvider = {
    ClientSSLEngineProvider { engine =>
      engine.setEnabledCipherSuites(Array("TLS_RSA_WITH_AES_256_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA"))
      engine.setEnabledProtocols(Array("SSLv3", "TLSv1"))
      engine
    }
  }

  implicit val longTimeout = 5.minutes
  // We need this for sending our HostConnectorSetup message below
  implicit val timeout = Timeout(longTimeout)

  /**
   * Fetch a page of campaign statistics from the Twilio API.
   *
   * @param apiUser The API user for Twilio
   * @param apiKey The API key for Twilio
   * @param isoCountry The ISO code for the country.
   *        If not set, return list of countries
   * @return a Validation containing either a Failure
   *         String or a T on Success
   */
  def getPhoneNumbers[T: FromResponseUnmarshaller](apiUser: String, apiKey: String, isoCountry: Option[String]): Validated[T] = {

    val path = buildRequestPath(isoCountry)

    val connectedSendReceive = for {
    Http.HostConnectorInfo(connector, _) <-
    IO(Http) ? Http.HostConnectorSetup(TwilioApiHost, TwilioApiPort, sslEncryption = true) //(sslEngineProvider)
    } yield sendReceive(connector)

    val pipeline = for (sendRecv <- connectedSendReceive) yield
      addHeader("Accept", "application/json")                         ~>
      addHeader(Authorization(BasicHttpCredentials(apiUser, apiKey))) ~>
      sendRecv                                                        ~>
      unmarshal[T]

    val req = Get(path)
    val future = pipeline.flatMap(_(req))
    val result = Await.ready(future, longTimeout).value.get // .get is safe because of Await.ready

    result match {
      case scala.util.Success(stats)                            => stats.success
      case scala.util.Failure(UnsuccessfulResponseException(r)) => parseFailure[T](r)
      case scala.util.Failure(err)                              => err.getMessage.fail
    }
  }

  /**
   * Shutdown procedure for Spray Client
   */
  def shutdown() {
    IO(Http).ask(Http.CloseAll)(1.second).await
    system.shutdown()
  }

  /**
   * Builds our API request path.
   *
   * @param isoCountry The ISO code for the country.
   *        If not set, return list of countries
   * @return our API request path
   */
  private[twilio] def buildRequestPath(isoCountry: Option[String]): String = {
    val root = s"/${TwilioApiVersion}/PhoneNumbers/Countries"
    isoCountry match {
      case Some(ic) => s"${root}/${ic}?PageSize=200"
      case None     => s"${root}?PageSize=200"
    }
  }

  /**
   * Checks our failure message.
   *
   * @param response Our HTTP response representing
   *        failure
   * @return a Validation boxing either an empty List
   *         on Success or a Failure String
   */
  private[twilio] def parseFailure[T: FromResponseUnmarshaller](response: HttpResponse): Validated[T] = {
    
    try {
      val json = parse(response.entity.asString)
      val err = json.extract[ErrorResult]
      s"API failure with message: ${err.message}".fail
    } catch {
      case _: Throwable =>
        (s"Error trying to recover from failure for response with Status: ${response.status}\n" +
        s"Body: ${if (response.entity.data.length < 1024) response.entity.asString else response.entity.data.length + " bytes"}").fail
    }
  }
}
