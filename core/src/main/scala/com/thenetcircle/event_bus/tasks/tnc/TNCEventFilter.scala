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

package com.thenetcircle.event_bus.tasks.tnc

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.thenetcircle.event_bus.context.{TaskBuildingContext, TaskRunningContext}
import com.thenetcircle.event_bus.interfaces.EventStatus.{NORM, SKIP}
import com.thenetcircle.event_bus.interfaces._
import com.thenetcircle.event_bus.misc.{Logging, Util}
import net.ceedubs.ficus.Ficus._

case class TNCEventFilterSettings(
    eventNameWhiteList: Seq[String] = Seq.empty,
    eventNameBlackList: Seq[String] = Seq.empty,
    channelWhiteList: Seq[String] = Seq.empty,
    channelBlackList: Seq[String] = Seq.empty,
    allowedTransportModes: Seq[String] = Seq.empty,
    onlyExtras: Map[String, String] = Map.empty
)

class TNCEventFilter(val settings: TNCEventFilterSettings) extends TransformTask with Logging {
  override def prepare()(
      implicit runningContext: TaskRunningContext
  ): Flow[Event, (EventStatus, Event), NotUsed] = Flow[Event].map(checkEvent)

  def checkEvent(event: Event): (EventStatus, Event) = {

    val eventBrief = Util.getBriefOfEvent(event)

    // match event name
    if (settings.eventNameBlackList.nonEmpty && event.metadata.name.nonEmpty) {
      // if the event name in event_name black list, then skip it
      if (settings.eventNameBlackList.exists(
            pattern => event.metadata.name.get matches pattern
          )) {
        consumerLogger.info(s"event $eventBrief is in event-name-black-list, skipped")
        return (SKIP, event)
      }
    }
    if (settings.eventNameWhiteList.nonEmpty) {
      if (event.metadata.name.isEmpty) {
        consumerLogger.info(s"event $eventBrief is not in event-name-white-list, skipped")
        return (SKIP, event)
      }
      // if the event name not in event_name white list, then skip it
      if (!settings.eventNameWhiteList.exists(
            pattern => event.metadata.name.get matches pattern
          )) {
        consumerLogger.info(s"event $eventBrief is not in event-name-white-list, skipped")
        return (SKIP, event)
      }
    }

    // match channel
    if (settings.channelBlackList.nonEmpty && event.metadata.channel.nonEmpty) {
      // if the event channel in channel black list, then skip it
      if (settings.channelBlackList.exists(
            pattern => event.metadata.channel.get matches pattern
          )) {
        consumerLogger.info(s"event $eventBrief is in channel-black-list, skipped")
        return (SKIP, event)
      }
    }
    if (settings.channelWhiteList.nonEmpty) {
      if (event.metadata.channel.isEmpty) {
        return (SKIP, event)
      }
      // if the event channel not in channel white list, then skip it
      if (!settings.channelWhiteList.exists(
            pattern => event.metadata.channel.get matches pattern
          )) {
        consumerLogger.info(s"event $eventBrief is not in channel-white-list, skipped")
        return (SKIP, event)
      }
    }

    // match transport mode
    if (settings.allowedTransportModes.nonEmpty) {
      val eventTransportMode = event.metadata.transportMode
      // if the event transport mode not in allowedTransportModes, then skip it
      val _predictor: String => Boolean = (tm: String) =>
        (tm.toUpperCase == "NONE" && eventTransportMode.isEmpty) || (eventTransportMode.nonEmpty && EventTransportMode
          .getFromString(tm) == eventTransportMode.get)

      if (!settings.allowedTransportModes.exists(_predictor)) {
        consumerLogger.info(s"event $eventBrief is not in allowed-transport-modes, skipped")
        return (SKIP, event)
      }
    }

    // match extras
    if (settings.onlyExtras.nonEmpty) {
      val eventExtras = event.metadata.extra
      // if the event extras do not match only extras, then skip it
      if (!settings.onlyExtras.forall {
            case (_key, _value) => eventExtras.get(_key).contains(_value)
          }) {
        consumerLogger.info(s"event $eventBrief does not match only-extras, skipped")
        return (SKIP, event)
      }
    }

    (NORM, event)
  }
}

class TNCEventFilterBuilder() extends TransformTaskBuilder {

  override def build(
      configString: String
  )(implicit buildingContext: TaskBuildingContext): TNCEventFilter = {
    val config = Util
      .convertJsonStringToConfig(configString)
      .withFallback(buildingContext.getSystemConfig().getConfig("task.tnc-event-filter"))

    val eventNameWhiteList    = config.as[Option[Seq[String]]]("event-name-white-list").getOrElse(Seq.empty)
    val eventNameBlackList    = config.as[Option[Seq[String]]]("event-name-black-list").getOrElse(Seq.empty)
    val channelWhiteList      = config.as[Option[Seq[String]]]("channel-white-list").getOrElse(Seq.empty)
    val channelBlackList      = config.as[Option[Seq[String]]]("channel-black-list").getOrElse(Seq.empty)
    val allowedTransportModes = config.as[Option[Seq[String]]]("allowed-transport-modes").getOrElse(Seq.empty)
    val onlyExtras            = config.as[Option[Map[String, String]]]("only-extras").getOrElse(Map.empty)

    val settings = TNCEventFilterSettings(
      eventNameWhiteList,
      eventNameBlackList,
      channelWhiteList,
      channelBlackList,
      allowedTransportModes,
      onlyExtras
    )

    new TNCEventFilter(settings)
  }

}
