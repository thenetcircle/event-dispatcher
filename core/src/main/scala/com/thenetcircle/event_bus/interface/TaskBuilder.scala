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

package com.thenetcircle.event_bus.interface

import com.thenetcircle.event_bus.story.TaskExecutingContext
import com.typesafe.config.{Config, ConfigFactory, ConfigParseOptions, ConfigSyntax}

trait TaskBuilder[+T <: Task] {

  def build(configString: String)(implicit context: TaskExecutingContext): T

  private val parseOptions = ConfigParseOptions.defaults().setSyntax(ConfigSyntax.JSON)
  def convertStringToConfig(configString: String): Config =
    ConfigFactory.parseString(configString.replaceAll("""\s*\#.*""", ""), parseOptions)

}

trait TaskABuilder extends TaskBuilder[TaskA]

trait TaskBBuilder extends TaskBuilder[TaskB]

trait TaskCBuilder extends TaskBuilder[TaskC]