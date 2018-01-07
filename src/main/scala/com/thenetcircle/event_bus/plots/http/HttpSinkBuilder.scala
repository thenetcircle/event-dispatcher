/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Beineng Ma <baineng.ma@gmail.com>
 */

package com.thenetcircle.event_bus.plots.http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import com.thenetcircle.event_bus.RunningContext
import com.thenetcircle.event_bus.interface.ISinkBuilder
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging

class HttpSinkBuilder() extends ISinkBuilder with StrictLogging {

  val defaultConfig: Config = convertStringToConfig("""
                                |{
                                |  "max-retry-times": 10,
                                |  "request": {
                                |    "port": 80,
                                |    "method": POST,
                                |    "uri": "/"
                                |  },
                                |  # "expected-response": "OK",
                                |  "akka.http.host-connection-pool": {
                                |    # "max-connections": 4,
                                |    "max-retries": 0,
                                |    # "max-open-requests": 32,
                                |    # "pipelining-limit": 1,
                                |    # "idle-timeout": "30s"
                                |  }
                                |}
                              """.stripMargin)

  override def build(configString: String)(implicit runningContext: RunningContext): HttpSink = {

    val config: Config = convertStringToConfig(configString).withFallback(defaultConfig)

    try {
      val requestMethod = config.getString("request.method").toUpperCase() match {
        case "POST" => HttpMethods.POST
        case "GET"  => HttpMethods.GET
        case unacceptedMethod =>
          throw new IllegalArgumentException(
            s"Http request method $unacceptedMethod is unsupported."
          )
      }
      val requsetUri = Uri(config.getString("request.uri"))
      val defaultRequest: HttpRequest = HttpRequest(method = requestMethod, uri = requsetUri)

      val expectedResponse =
        if (config.hasPath("expected-response-data"))
          Some(config.getString("expected-response-data"))
        else None

      val connectionPoolSettings =
        if (config.hasPath("akka.http.host-connection-pool"))
          Some(ConnectionPoolSettings(config.withFallback(runningContext.appContext.getConfig())))
        else None

      new HttpSink(
        HttpSinkSettings(
          config.getString("request.host"),
          config.getInt("request.port"),
          config.getInt("max-retry-times"),
          defaultRequest,
          expectedResponse,
          connectionPoolSettings
        )
      )

    } catch {
      case ex: Throwable =>
        logger.error(s"Creating HttpSinkSettings failed with error: ${ex.getMessage}")
        throw ex
    }

  }
}