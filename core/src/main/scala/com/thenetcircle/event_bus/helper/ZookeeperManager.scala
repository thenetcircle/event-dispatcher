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

package com.thenetcircle.event_bus.helper

import com.typesafe.scalalogging.StrictLogging
import org.apache.curator.framework.imps.CuratorFrameworkState
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.zookeeper.CreateMode

import scala.collection.JavaConverters._

class ZookeeperManager(connectString: String, rootPath: String) extends StrictLogging {

  val client: CuratorFramework =
    CuratorFrameworkFactory.newClient(connectString, new ExponentialBackoffRetry(1000, 3))

  def start(): Unit = {
    client.start()
    sys.addShutdownHook(if (client.getState == CuratorFrameworkState.STARTED) client.close())

    // Check and Create root nodes
    if (client.checkExists().forPath(getAbsPath("stories")) == null) {
      client.create().creatingParentsIfNeeded().forPath(getAbsPath("stories"))
    }
    if (client.checkExists().forPath(getAbsPath("runners")) == null) {
      client.create().creatingParentsIfNeeded().forPath(getAbsPath("runners"))
    }
  }

  def close(): Unit = if (client.getState == CuratorFrameworkState.STARTED) client.close()

  def getAbsPath(relativePath: String): String = s"$rootPath/$relativePath"

  def registerStoryRunner(runnerName: String): String = {
    val payload = ""
    client
      .create()
      .creatingParentsIfNeeded()
      .withProtection()
      .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
      .forPath(getAbsPath(s"runners/$runnerName/member"), payload.getBytes())
  }

  def getClient(): CuratorFramework = client

  def getData(relativePath: String): Option[String] = {
    try {
      Some(client.getData.forPath(relativePath).mkString)
    } catch {
      case e: Throwable =>
        logger.info(s"getData from $relativePath failed with error: ${e.getMessage}")
        None
    }
  }

  def getChildren(relativePath: String): Option[List[String]] = {
    try {
      Some(
        client.getChildren
          .forPath(getAbsPath(relativePath))
          .asScala
          .toList
      )
    } catch {
      case e: Throwable =>
        logger.info(s"getChildren from $relativePath failed with error: ${e.getMessage}")
        None
    }
  }

  def getChildrenData(relativePath: String): Option[List[(String, String)]] = {
    getChildren(relativePath).map(
      _.flatMap(childName => getData(childName).map(data => childName -> data))
    )
  }

  /*def requestLeadership(relativePath: String,
                        callback: (LeaderSelector, ZKManager) => Unit): Unit = {
    val zkManager = this
    val leaderSelector =
      new LeaderSelector(client, getAbsPath(relativePath), new LeaderSelectorListenerAdapter {
        override def takeLeadership(client: CuratorFramework): Unit = {
          callback(this, zkManager)
        }
      })
    leaderSelector.start()
  }*/

}

object ZookeeperManager {
  def apply(connectString: String, rootPath: String): ZookeeperManager = {
    if (connectString.isEmpty || rootPath.isEmpty) {
      throw new IllegalArgumentException("Parameters are unavailable.")
    }
    new ZookeeperManager(connectString, rootPath)
  }
}