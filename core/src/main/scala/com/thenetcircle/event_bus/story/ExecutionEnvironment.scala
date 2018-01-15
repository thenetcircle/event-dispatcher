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
import com.thenetcircle.event_bus.misc.Environment
import com.typesafe.config.Config

class ExecutionEnvironment(executorGroupName: String,
                           executorId: String,
                           appName: String,
                           appVersion: String,
                           appEnv: String,
                           debug: Boolean,
                           systemConfig: Config,
                           actorSystem: ActorSystem)
    extends Environment(
      appName: String,
      appVersion: String,
      appEnv: String,
      debug: Boolean,
      systemConfig: Config
    ) {

  def getExecutorGroupName(): String = executorGroupName
  def getExecutorId(): String = executorId
  def getActorSystem(): ActorSystem = actorSystem

}

object ExecutionEnvironment {

  def apply(
      executorGroupName: String,
      executorId: String
  )(implicit environment: Environment, system: ActorSystem): ExecutionEnvironment = {

    new ExecutionEnvironment(
      executorGroupName,
      executorId,
      environment.getAppName(),
      environment.getAppVersion(),
      environment.getAppEnv(),
      environment.isDebug(),
      environment.getConfig(),
      system
    )

  }

}
