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

package com.thenetcircle.event_bus.source

import akka.kafka.ConsumerMessage.{ CommittableOffset, CommittableOffsetBatch }
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ AutoSubscription, Subscriptions }
import akka.stream.scaladsl.{ Flow, Source }
import akka.{ Done, NotUsed }
import com.thenetcircle.event_bus.driver.adapter.KafkaSourceAdapter
import com.thenetcircle.event_bus.driver.extractor.Extractor
import com.thenetcircle.event_bus.{ Event, EventFmt }

object KafkaSource {

  def atLeastOnce[Fmt <: EventFmt](
      settings: KafkaSourceSettings
  )(implicit extractor: Extractor[Fmt]): Source[Event, Consumer.Control] = {
    val consumerName = settings.name
    val consumerSettings = settings.consumerSettings

    var subscription: AutoSubscription = if (settings.topics.isDefined) {
      Subscriptions.topics(settings.topics.get)
    } else if (settings.topicPattern.isDefined) {
      Subscriptions.topicPattern(settings.topicPattern.get)
    } else {
      throw new IllegalArgumentException("Kafka source need subscribe topics")
    }

    Consumer
      .committableSource(consumerSettings, subscription)
      .map(msg => {
        KafkaSourceAdapter.fit(msg.record).addContext("committableOffset", msg.committableOffset)
      })
      .map(extractor.extract)
  }

  def commit(parallelism: Int = 3, batchMax: Int = 20): Flow[Event, Done, NotUsed] =
    Flow[Event]
      .map(_.rawEvent.context("committableOffset").asInstanceOf[CommittableOffset])
      .batch(max = batchMax, first => CommittableOffsetBatch.empty.updated(first)) { (batch, elem) =>
        batch.updated(elem)
      }
      .mapAsync(parallelism)(_.commitScaladsl())

}
