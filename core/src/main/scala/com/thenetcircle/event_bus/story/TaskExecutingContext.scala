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

package com.thenetcircle.event_bus.story

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.thenetcircle.event_bus.Environment

import scala.concurrent.ExecutionContext

class TaskExecutingContext(environment: Environment,
                           system: ActorSystem,
                           materializer: Materializer,
                           executor: ExecutionContext,
                           builderFactory: TaskBuilderFactory) {

  def getEnvironment(): Environment = environment
  def getActorSystem(): ActorSystem = system
  def getMaterializer(): Materializer = materializer
  def getExecutor(): ExecutionContext = executor
  def getBuilderFactory(): TaskBuilderFactory = builderFactory

}