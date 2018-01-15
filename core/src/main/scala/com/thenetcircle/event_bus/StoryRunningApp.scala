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

package com.thenetcircle.event_bus

import akka.actor.ActorSystem
import com.thenetcircle.event_bus.misc.{BaseEnvironment, ZKManager}
import com.thenetcircle.event_bus.story._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.Await
import scala.concurrent.duration._

object StoryRunningApp extends App with StrictLogging {

  logger.info("Application is initializing.")

  // Initialize BaseEnvironment
  val baseEnvironment: BaseEnvironment = BaseEnvironment(ConfigFactory.load())

  // Check Executor Name
  var runnerGroup: String = if (args.length > 0) args(0) else ""
  if (runnerGroup.isEmpty)
    runnerGroup = baseEnvironment.getConfig().getString("app.default-runner-group")

  // Connecting Zookeeper
  val zKManager: ZKManager = ZKManager(baseEnvironment.getConfig())(baseEnvironment)
  zKManager.init()
  val runnerId: String = zKManager.registerStoryExecutor(runnerGroup)

  // Create ActorSystem
  implicit val system: ActorSystem =
    ActorSystem(baseEnvironment.getAppName(), baseEnvironment.getConfig())

  implicit val runningEnvironment: RunningEnvironment =
    RunningEnvironment(runnerGroup, runnerId)(baseEnvironment, system)

  // Kamon.start()

  // Schedule Stories
  val storyManager = StoryManager(zKManager, TaskBuilderFactory(runningEnvironment.getConfig()))
  StoryScheduler(storyManager).execute()

  sys.addShutdownHook({
    logger.info("Application is shutting down...")
    // Kamon.shutdown()
    runningEnvironment.shutdown()
    system.terminate()
    Await.result(system.whenTerminated, 60.seconds)
    /*Http()
      .shutdownAllConnectionPools()
      .map(_ => {
        globalActorSystem.terminate()
        Await.result(globalActorSystem.whenTerminated, 60.seconds)
      })(ExecutionContext.global)*/
  })

}
