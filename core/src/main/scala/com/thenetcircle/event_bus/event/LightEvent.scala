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

package com.thenetcircle.event_bus.event

import java.time.Instant
import java.util.Date

import com.thenetcircle.event_bus.event.extractor.DataFormat
import com.thenetcircle.event_bus.interfaces.{Event, EventBody, EventMetaData}

case class LightEvent(uuid: String = "failure-event-" + java.util.UUID.randomUUID().toString,
                      metadata: EventMetaData = EventMetaData(),
                      body: EventBody = EventBody("", DataFormat.UNKNOWN),
                      createdAt: Date = Date.from(Instant.now()),
                      passThrough: Option[Any] = None)
    extends Event {

  def withPassThrough[T](_passThrough: T): Event = {
    if (passThrough.isDefined) {
      throw new Exception("event passthrough is defined already.")
    }
    copy(passThrough = Some(_passThrough))
  }

  def withGroup(_group: String): Event = copy(metadata = metadata.copy(group = Some(_group)))

}

object LightEvent {
  def apply(ex: Throwable): LightEvent =
    new LightEvent(metadata = EventMetaData(provider = Some(ex.getClass.getName -> ex.getMessage)))
}
