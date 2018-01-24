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

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.thenetcircle.event_bus.Core
import com.thenetcircle.event_bus.misc.ZKManager
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.Await
import scala.concurrent.duration._

class Main extends Core {

  val config: Config = ConfigFactory.load()

  // Setup Zookeeper
  val zkManager: ZKManager = ZKManager(config.getString("app.zkserver"), config.getString("app.zkroot"))
  zkManager.start()
  appContext.setZKManager(zkManager)

  def run(args: Array[String]): Unit = {

    implicit val materializer = ActorMaterializer()
    import scala.concurrent.ExecutionContext.Implicits.global

    val route: Route  = getRoute()
    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8990)

    appContext.addShutdownHook(Await.result(bindingFuture.map(_.unbind()), 5.seconds))

  }

  def getRoute(): Route =
    pathSingleSlash {
      // homepage
      complete("event-bus admin is running!")
    } ~
    get {
      path("zk/copy") {
        complete("event-bus admin is running!")
      }
    }
}

object Main extends App { (new Main).run(args) }
