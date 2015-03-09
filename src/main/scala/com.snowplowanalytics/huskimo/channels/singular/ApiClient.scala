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
package singular

// Java
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManager
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

// Joda-Time
import org.joda.time.DateTime

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
import spray.httpx.{UnsuccessfulResponseException => UrUnsuccessfulResponseException}
import spray.httpx.unmarshalling.FromResponseUnmarshaller
import spray.util._
import spray.client.pipelining._
import spray.can.Http
import spray.can.Http.HostConnectorSetup
import spray.io.ClientSSLEngineProvider

// This project
import utils.{
  ConversionUtils,
  DateTimeFormatters
}
import ApiProtocol._

object UnsuccessfulResponseException {
  def unapply(a: UrUnsuccessfulResponseException): Option[HttpResponse]
    = Some(a.response)
}

/**
 * Contains the Singular API-related tasks required
 * for loading the campaign statistics.
 */
object ApiClient {

  private val SingularApiHost = "api.singular.net"
  private val SingularApiPort = 443

  private val Encoding = "UTF-8"

  // We need an ActorSystem to host our application in
  implicit val system = ActorSystem(generated.ProjectSettings.name)
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
   * Fetch a page of campaign statistics from the Singular API.
   *
   * @param apiKey The API key for Singular
   * @param resourceSlug The slug of the resource
   *        to request from Singular
   * @param endDate The last day to retrieve
   *        campaign statistics for
   * @return a Validation containing either a Failure
   *         String or a List (possibly empty) of
   *         CampaignStatistics on Success.
   */
  def getStatistics[T: FromResponseUnmarshaller](apiKey: String, resourceSlug: String, date: DateTime): Validated[T] = {

    log.info(s"Fetching ${resourceSlug} data")

    val path = buildRequestPath(apiKey, resourceSlug, date)

    val connectedSendReceive = for {
    Http.HostConnectorInfo(connector, _) <-
    IO(Http) ? Http.HostConnectorSetup(SingularApiHost, SingularApiPort, sslEncryption = true) //(sslEngineProvider)
    } yield sendReceive(connector)

    val pipeline = for (sendRecv <- connectedSendReceive) yield
      addHeader("Accept", "application/json") ~>
      sendRecv                                ~>
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
   * @param apiKey The API key for Singular
   * @param resourceSlug The slug of the resource
   *        to request from Singular
   * @param date The date to retrieve campaign
   *        data for
   * @return our API request path
   */
  private[singular] def buildRequestPath(apiKey: String, resourceSlug: String, date: DateTime): String = {
    val dt = DateTimeFormatters.YyyyMmDd.print(date)
    s"/api/${apiKey}/adv/${resourceSlug}/${dt}/${dt}"
  }

  /**
   * Checks our failure message.
   *
   * @param response Our HTTP response representing
   *        failure
   * @return a Validation boxing either an empty List
   *         on Success or a Failure String
   */
  private[singular] def parseFailure[T: FromResponseUnmarshaller](response: HttpResponse): Validated[T] = {
    
    try {
      val json = parse(response.entity.asString)
      val err = json.extract[ErrorResult]
      s"API failure with message: ${err.error}".fail
    } catch {
      case _: Throwable =>
        (s"Error trying to recover from failure for response with Status: ${response.status}\n" +
        s"Body: ${if (response.entity.data.length < 1024) response.entity.asString else response.entity.data.length + " bytes"}").fail
    }
  }
}
