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

package com.thenetcircle.event_bus.misc

import com.thenetcircle.event_bus.context.AppContext
import com.thenetcircle.event_bus.interfaces.EventStatus.{Fail, InFB, Norm, ToFB}
import com.thenetcircle.event_bus.interfaces.{Event, EventStatus}
import kamon.Kamon
import kamon.metric.instrument.InstrumentFactory
import kamon.metric.{EntityRecorderFactory, GenericEntityRecorder}

object Monitor {
  def init()(implicit appContext: AppContext): Monitor = {
    val monitor = new Monitor()
    appContext.setMonitor(monitor)
    monitor
  }

  class EventMetrics(instrumentFactory: InstrumentFactory) extends GenericEntityRecorder(instrumentFactory) {
    val normal     = counter("normal")
    val inFallback = counter("in-fallback")
    val toFallback = counter("to-fallback")
    val failure    = counter("failure")
  }

  object EventMetrics extends EntityRecorderFactory[EventMetrics] {
    def category: String                                                   = "event"
    def createRecorder(instrumentFactory: InstrumentFactory): EventMetrics = new EventMetrics(instrumentFactory)
  }
}

class Monitor()(implicit appContext: AppContext) {

  import Monitor._

  val isKamonEnabled: Boolean = appContext.getSystemConfig().getString("app.monitor.kamon.auto-start") == "yes"

  if (isKamonEnabled) {
    Kamon.start()
    appContext.addShutdownHook(Kamon.shutdown())
  }

  def traceEvent(status: EventStatus, event: Event, storyName: String): Unit = if (isKamonEnabled) {
    val eventEntity = Kamon.metrics.entity(EventMetrics, storyName)
    status match {
      case Norm =>
        eventEntity.normal.increment()
      case ToFB(opEx) =>
        eventEntity.toFallback.increment()
      case InFB =>
        eventEntity.inFallback.increment()
      case Fail(ex) =>
        eventEntity.failure.increment()
    }
  }

}