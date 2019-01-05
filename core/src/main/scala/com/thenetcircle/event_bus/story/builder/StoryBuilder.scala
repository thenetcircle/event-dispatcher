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

package com.thenetcircle.event_bus.story.builder

import com.thenetcircle.event_bus.context.{AppContext, TaskBuildingContext}
import com.thenetcircle.event_bus.story.{Story, StorySettings, StoryStatus}
import com.thenetcircle.event_bus.story.interfaces.{IFallbackTask, ISinkTask, ISourceTask, ITransformTask}
import com.typesafe.scalalogging.LazyLogging

import scala.util.matching.Regex

class StoryBuilder(taskBuilderFactory: TaskBuilderFactory, taskBuildingContext: TaskBuildingContext)(
    implicit appContext: AppContext
) extends LazyLogging {

  implicit val _context: TaskBuildingContext = taskBuildingContext

  val categoryDelimiter = """#"""
  val taskDelimiter     = """|||"""

  def buildStory(rawData: StoryRawData): Story =
    try {
      new Story(
        StorySettings(rawData.name, StoryStatus(rawData.status)),
        buildSourceTask(rawData.source).get,
        buildSinkTask(rawData.sink).get,
        rawData.transforms.map(_tcs => {
          _tcs
            .split(Regex.quote(taskDelimiter))
            .map(_cs => buildTransformTask(_cs).get)
            .toList
        }),
        rawData.fallback.map(buildFallbackTask(_).get)
      )
    } catch {
      case ex: Throwable =>
        logger.error(s"story ${rawData.name} build failed with error $ex")
        throw ex
    }

  def parseConfigString(configString: String): (String, String) = {
    val re = configString.split(Regex.quote(categoryDelimiter), 2)
    (re(0), if (re.length == 2) re(1) else "{}")
  }

  def buildSourceTask(configString: String): Option[ISourceTask] = {
    val (_category, _config) = parseConfigString(configString)
    buildSourceTask(_category, _config)
  }

  def buildSourceTask(category: String, configString: String): Option[ISourceTask] =
    taskBuilderFactory.getSourceTaskBuilder(category).map(_builder => _builder.build(configString))

  def buildTransformTask(configString: String): Option[ITransformTask] = {
    val (_category, _config) = parseConfigString(configString)
    buildTransformTask(_category, _config)
  }

  def buildTransformTask(category: String, configString: String): Option[ITransformTask] =
    taskBuilderFactory
      .getTransformTaskBuilder(category)
      .map(_builder => _builder.build(configString))

  def buildSinkTask(configString: String): Option[ISinkTask] = {
    val (_category, _config) = parseConfigString(configString)
    buildSinkTask(_category, _config)
  }

  def buildSinkTask(category: String, configString: String): Option[ISinkTask] =
    taskBuilderFactory.getSinkTaskBuilder(category).map(_builder => _builder.build(configString))

  def buildFallbackTask(configString: String): Option[IFallbackTask] = {
    val (_category, _config) = parseConfigString(configString)
    buildFallbackTask(_category, _config)
  }

  def buildFallbackTask(category: String, configString: String): Option[IFallbackTask] =
    taskBuilderFactory
      .getFallbackTaskBuilder(category)
      .map(_builder => _builder.build(configString))
}
