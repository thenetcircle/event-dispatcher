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

package com.thenetcircle.event_bus.transporter.entrypoint
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.thenetcircle.event_bus.event_extractor.EventExtractor
import com.thenetcircle.event_bus.transporter.entrypoint.EntryPointPriority.EntryPointPriority
import com.thenetcircle.event_bus.{Event, EventFormat}
import com.typesafe.config.{Config, ConfigException}

sealed trait EntryPointSettings {
  def name: String
  def priority: EntryPointPriority
  def eventFormat: EventFormat
}

object EntryPointSettings {

  /** Returns a [[EntryPointSettings]] from a TypeSafe [[Config]]
    *
    * @throws ConfigException
    *             if config incorrect
    * @throws IllegalArgumentException
    *             if "type" didn't match any predefined types
    */
  def apply(config: Config): EntryPointSettings = {
    var entryPointType = config.getString("type")

    entryPointType.toUpperCase() match {
      case "HTTP" =>
        val priority =
          if (config.hasPath("priority"))
            EntryPointPriority(config.getInt("priority"))
          else EntryPointPriority.Normal

        // TODO: adjust these default values when doing stress testing
        // TODO: use reference.conf to set up default value
        val maxConnections =
          if (config.hasPath("max-connections"))
            config.getInt("max-connections")
          else 1000

        val perConnectionParallelism =
          if (config.hasPath("pre-connection-parallelism"))
            config.getInt("pre-connection-parallelism")
          else 10

        val eventFormat =
          if (config.hasPath("format"))
            EventFormat(config.getString("format"))
          else EventFormat.DefaultFormat

        HttpEntryPointSettings(
          config.getString("name"),
          priority,
          maxConnections,
          perConnectionParallelism,
          eventFormat,
          config.getString("interface"),
          config.getInt("port")
        )
      case _ =>
        throw new IllegalArgumentException(
          """EntryPoint "type" is not correct!""")
    }
  }

}

/** Http EntryPoint Settings */
case class HttpEntryPointSettings(
    name: String,
    priority: EntryPointPriority,
    maxConnections: Int,
    perConnectionParallelism: Int,
    eventFormat: EventFormat,
    interface: String,
    port: Int
) extends EntryPointSettings

/** Abstraction Api of All EntryPoints */
trait EntryPoint {
  val name: String
  val priority: EntryPointPriority
  val eventFormat: EventFormat

  // TODO: add switcher as the materialized value
  def port: Source[Event, _]
}

/** Create a new EntryPoint based on it's settings
  */
object EntryPoint {

  def apply(settings: EntryPointSettings)(
      implicit system: ActorSystem,
      materializer: Materializer,
      eventExtractor: EventExtractor): EntryPoint = settings match {
    case s: HttpEntryPointSettings =>
      HttpEntryPoint(s)
  }

}

object EntryPointPriority extends Enumeration {
  type EntryPointPriority = Value
  val High   = Value(6, "High")
  val Normal = Value(3, "Normal")
  val Low    = Value(1, "Low")
}
