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

package com.thenetcircle.event_bus.interfaces

import com.thenetcircle.event_bus.context.TaskRunningContext
import com.thenetcircle.event_bus.interfaces.EventStatus.{Fail, Norm}

import scala.util.{Failure, Success, Try}

trait Task {
  type Status = EventStatus

  def createStatusFromTry[T](that: Try[T], ss: Status = Norm): Status = that match {
    case Success(_)  => ss
    case Failure(ex) => Fail(ex)
  }

  def shutdown()(implicit runningContext: TaskRunningContext): Unit
}
