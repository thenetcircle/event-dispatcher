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
import akka.actor.{ActorRef, ActorSystem}
import com.thenetcircle.event_bus.context.AppContext
import com.thenetcircle.event_bus.story.StoryRunner
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.Await
import scala.concurrent.duration._

trait AbstractApp extends StrictLogging {

  def initCoreComponents(config: Config,
                         runnerName: String): (AppContext, ActorSystem, ActorRef) = {
    // Base components
    implicit val appContext: AppContext = AppContext(config)
    implicit val system: ActorSystem = ActorSystem(appContext.getAppName(), config)

    // Initialize StoryRunner
    val storyRunner: ActorRef =
      system.actorOf(StoryRunner.props(runnerName), "runner-" + runnerName)

    // Setup shutdown hooks
    sys.addShutdownHook({
      logger.info("Application is shutting down...")
      Await
        .result(
          akka.pattern.gracefulStop(storyRunner, 3.seconds, StoryRunner.Shutdown()),
          3.seconds
        )
      appContext.shutdown()
      system.terminate()
      Await.result(system.whenTerminated, 6.seconds)
    })

    (appContext, system, storyRunner)
  }

}
