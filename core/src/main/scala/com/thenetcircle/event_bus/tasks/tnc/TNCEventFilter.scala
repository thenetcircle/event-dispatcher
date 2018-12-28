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
import com.thenetcircle.event_bus.interfaces.{Event, EventStatus, TransformTask, TransformTaskBuilder}
import com.thenetcircle.event_bus.misc.{Logging, Util}

class TNCEventFilter() extends TransformTask with Logging {
  override def prepare()(
      implicit runningContext: TaskRunningContext
  ): Flow[Event, (EventStatus, Event), NotUsed] = ???
}

class TNCEventFilterBuilder() extends TransformTaskBuilder {

  override def build(
      configString: String
  )(implicit buildingContext: TaskBuildingContext): TNCEventFilter = {
    val config = Util
      .convertJsonStringToConfig(configString)
      .withFallback(buildingContext.getSystemConfig().getConfig("task.tnc-topic-resolver"))

    val zkMangerOption = buildingContext.getAppContext().getZooKeeperManager()
    if (zkMangerOption.isEmpty) {
      throw new IllegalArgumentException("ZooKeeperManager is required for TNCKafkaTopicResolver")
    }

    new TNCEventFilter()
  }

}