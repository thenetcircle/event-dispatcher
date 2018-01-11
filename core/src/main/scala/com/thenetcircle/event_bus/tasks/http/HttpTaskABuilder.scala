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

package com.thenetcircle.event_bus.tasks.http

import akka.http.scaladsl.settings.ServerSettings
import com.thenetcircle.event_bus.event.extractor.DataFormat.DataFormat
import com.thenetcircle.event_bus.interface.TaskABuilder
import com.thenetcircle.event_bus.misc.ConfigStringParser
import com.thenetcircle.event_bus.story.TaskContext
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import net.ceedubs.ficus.Ficus._

class HttpTaskABuilder() extends TaskABuilder with StrictLogging {

  val defaultConfig: Config = ConfigStringParser.convertStringToConfig(
    """ 
      |{
      |  # "interface": "...",
      |  "port": 8000,
      |  # "akka": {
      |  #   "http": {
      |  #     "server": {} // override "akka.http.server" default settings
      |  #   }
      |  # }
      |  "succeeded-response": "ok",
      |  "error-response": "ko",
      |  "format": "ActivityStreams",
      |  "max-connections": 1000,
      |  "pre-connection-parallelism": 10
      |}
    """.stripMargin
  )

  override def build(configString: String)(implicit context: TaskContext): HttpTaskA = {

    try {
      val config: Config =
        ConfigStringParser.convertStringToConfig(configString).withFallback(defaultConfig)

      val serverSettingsOption: Option[ServerSettings] =
        if (config.hasPath("akka.http.server"))
          Some(ServerSettings(config.withFallback(context.getEnvironment().getConfig())))
        else None

      new HttpTaskA(
        HttpTaskASettings(
          config.as[String]("interface"),
          config.as[Int]("port"),
          config.as[DataFormat]("format"),
          config.as[Int]("max-connections"),
          config.as[Int]("pre-connection-parallelism"),
          config.as[String]("succeeded-response"),
          config.as[String]("error-response"),
          serverSettingsOption
        )
      )

    } catch {
      case ex: Throwable =>
        logger.error(s"Creating a HttpTaskA failed with error: ${ex.getMessage}")
        throw ex
    }
  }
}
