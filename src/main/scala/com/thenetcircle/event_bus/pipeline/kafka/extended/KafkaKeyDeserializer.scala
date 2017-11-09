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

package com.thenetcircle.event_bus.pipeline.kafka.extended

import java.util

import com.thenetcircle.event_bus.pipeline.kafka.KafkaKey
import org.apache.kafka.common.serialization.Deserializer

class KafkaKeyDeserializer extends Deserializer[KafkaKey] {
  override def deserialize(topic: String, data: Array[Byte]): KafkaKey =
    new KafkaKey(new String(data, "UTF-8"))

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}
  override def close(): Unit = {}
}
