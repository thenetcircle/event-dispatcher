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

import akka.actor.ActorRef
import akka.pattern.gracefulStop
import com.thenetcircle.event_bus.misc.{Monitor, ZooKeeperManager}
import com.thenetcircle.event_bus.story.{StoryBuilder, StoryRunner, StoryZooKeeperListener, TaskBuilderFactory}
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.Await
import scala.concurrent.duration._

class Core extends AbstractApp {

  val config: Config = ConfigFactory.load()

  def run(args: Array[String]): Unit = {

    ZooKeeperManager.init()
    Monitor.init()

    // Initialize StoryRunner
    val runnerName: String = config.getString("app.runner-name")
    val storyRunner: ActorRef =
      system.actorOf(StoryRunner.props(runnerName), "runner-" + runnerName)
    appContext.addShutdownHook {
      Await.ready(
        gracefulStop(storyRunner, 3.seconds, StoryRunner.Shutdown()),
        3.seconds
      )
    }

    val storyBuilder: StoryBuilder = StoryBuilder(TaskBuilderFactory(appContext.getSystemConfig()))

    StoryZooKeeperListener(runnerName, storyRunner, storyBuilder).start()

  }
}

object Core extends App { (new Core).run(args) }
