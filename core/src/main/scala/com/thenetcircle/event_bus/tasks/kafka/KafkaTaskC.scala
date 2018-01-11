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

package com.thenetcircle.event_bus.tasks.kafka

import akka.NotUsed
import akka.kafka.ProducerMessage.Message
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Flow
import com.thenetcircle.event_bus.event.Event
import com.thenetcircle.event_bus.interface.TaskC
import com.thenetcircle.event_bus.tasks.kafka.extended.{KafkaKey, KafkaPartitioner}
import com.thenetcircle.event_bus.story.TaskExecutingContext
import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.clients.producer.{ProducerConfig, ProducerRecord}

case class KafkaTaskCSettings(producerSettings: ProducerSettings[ProducerKey, ProducerValue])

class KafkaTaskC(val settings: KafkaTaskCSettings)(implicit context: TaskExecutingContext)
    extends TaskC
    with StrictLogging {

  import KafkaTaskC._

  private val producerSettings = settings.producerSettings
    .withProperty(ProducerConfig.PARTITIONER_CLASS_CONFIG, classOf[KafkaPartitioner].getName)

  override def getGraph(): Flow[Event, Event, NotUsed] =
    Flow[Event]
      .map(event => {
        Message(getProducerRecordFromEvent(event), event)
      })
      // TODO: take care of Supervision of mapAsync
      .via(Producer.flow(producerSettings))
      .map(msg => msg.message.passThrough)
}

object KafkaTaskC {
  private def getProducerRecordFromEvent(
      event: Event
  ): ProducerRecord[ProducerKey, ProducerValue] = {
    // TODO: use channel detective
    val topic: String = event.metadata.channel.getOrElse("event-default")
    val timestamp: Long = event.metadata.published
    val key: ProducerKey = KafkaKey(event)
    val value: ProducerValue = event

    new ProducerRecord[ProducerKey, ProducerValue](
      topic,
      null,
      timestamp.asInstanceOf[java.lang.Long],
      key,
      value
    )
  }
}