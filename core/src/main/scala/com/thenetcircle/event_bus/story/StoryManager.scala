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

import akka.actor.{ActorRef, ActorSystem}
import com.thenetcircle.event_bus.misc.DaoFactory
import com.thenetcircle.event_bus.story.StoryDAO.StoryInfo

import scala.collection.mutable

class StoryManager(
    daoFactory: DaoFactory,
    taskBuilderFactory: TaskBuilderFactory,
    taskContextFactory: TaskContextFactory
)(implicit environment: ExecutionEnvironment) {

  implicit val system: ActorSystem = environment.getActorSystem()
  private val storyDAO: StoryDAO = daoFactory.getStoryDAO()
  private val runningStories: mutable.Set[ActorRef] = mutable.Set.empty

  def execute(): Unit = {
    val candidateStories: List[String] =
      storyDAO.getAvailableStories(environment.getExecutorGroupName())

    candidateStories.foreach(storyName => {
      val story = createStory(storyName)
      val storyContainer = system.actorOf(StoryContainer.props(story), storyName)

      runningStories += storyContainer
    })
  }

  def createStory(storyName: String): Story = {
    val storyInfo: StoryInfo = storyDAO.getStoryInfo(storyName)
    implicit val taskContext: TaskContext = taskContextFactory.newTaskExecutingContext()
    new Story(
      storyInfo.name,
      StorySettings(),
      StoryStatus(storyInfo.status),
      taskBuilderFactory.buildSourceTask(storyInfo.source).get,
      taskBuilderFactory.buildSinkTask(storyInfo.sink).get,
      storyInfo.transforms.map(_.flatMap(_v => taskBuilderFactory.buildTransformTask(_v))),
      storyInfo.fallbacks.map(_.flatMap(_v => taskBuilderFactory.buildSinkTask(_v)))
    )
  }

}

object StoryManager {
  def apply(
      daoFactory: DaoFactory,
      taskBuilderFactory: TaskBuilderFactory,
      taskContextFactory: TaskContextFactory
  )(implicit environment: ExecutionEnvironment): StoryManager =
    new StoryManager(daoFactory, taskBuilderFactory, taskContextFactory)
}
