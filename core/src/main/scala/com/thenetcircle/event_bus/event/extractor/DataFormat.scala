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

package com.thenetcircle.event_bus.event.extractor
import com.typesafe.config.Config
import net.ceedubs.ficus.readers.ValueReader

object DataFormat extends Enumeration {
  type DataFormat = Value

  val TEST = Value(-1, "TEST")
  val ACTIVITYSTREAMS = Value(1, "ACTIVITYSTREAMS")

  def apply(name: String): DataFormat = name.toUpperCase match {
    case "ACTIVITYSTREAMS" => ACTIVITYSTREAMS
  }

  implicit val eventFormatReader: ValueReader[DataFormat] =
    new ValueReader[DataFormat] {
      override def read(config: Config, path: String) =
        DataFormat(config.getString(path))
    }
}