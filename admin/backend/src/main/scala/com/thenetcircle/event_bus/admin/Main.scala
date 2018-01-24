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

package com.thenetcircle.event_bus.admin

import com.thenetcircle.event_bus.Core
import com.thenetcircle.event_bus.misc.ZKManager
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging

object Main extends Core with App with StrictLogging {

  val config: Config = ConfigFactory.load()

  // Setup Zookeeper
  val zkManager: ZKManager = ZKManager(config.getString("app.zkserver"), config.getString("app.zkroot"))
  zkManager.start()
  appContext.setZKManager(zkManager)

}
