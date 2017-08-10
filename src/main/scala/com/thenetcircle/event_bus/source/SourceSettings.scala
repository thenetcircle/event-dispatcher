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

import akka.kafka.ConsumerSettings
import com.thenetcircle.event_bus.driver.{ KafkaKey, KafkaValue }
import com.thenetcircle.event_bus.alpakka.redis.RedisConnectionSettings

sealed trait SourceSettings

final case class RedisPubSubSourceSettings(
    connectionSettings: RedisConnectionSettings,
    channels: Seq[String] = Seq.empty,
    patterns: Seq[String] = Seq.empty,
    bufferSize: Int = 10
) extends SourceSettings {

  def withBufferSize(bufferSize: Int): RedisPubSubSourceSettings =
    copy(bufferSize = bufferSize)

  def withChannels(channels: Seq[String]): RedisPubSubSourceSettings =
    copy(channels = channels)

  def withPatterns(patterns: Seq[String]): RedisPubSubSourceSettings =
    copy(patterns = patterns)

}

final case class KafkaSourceSettings(
    consumerSettings: ConsumerSettings[KafkaKey, KafkaValue],
    topics: Option[Set[String]] = None,
    topicPattern: Option[String] = None,
    name: String = "DefaultKafkaSource"
) extends SourceSettings
