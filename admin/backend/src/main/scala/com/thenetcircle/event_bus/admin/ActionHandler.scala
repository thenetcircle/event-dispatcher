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

import akka.actor.ActorSystem
import com.thenetcircle.event_bus.context.AppContext
import com.thenetcircle.event_bus.misc.ZooKeeperManager
import com.typesafe.config.{ConfigFactory, ConfigObject}
import com.typesafe.scalalogging.StrictLogging
import spray.json.DefaultJsonProtocol

import scala.collection.JavaConverters._

case class StoryInfo(
    name: String,
    source: Option[String],
    sink: Option[String],
    status: Option[String],
    transforms: Option[String],
    fallback: Option[String]
)

object StoryInfoJsonSupport extends DefaultJsonProtocol {
  implicit val StoryInfoFormats = jsonFormat6(StoryInfo)
}

class ActionHandler(zkManager: ZooKeeperManager)(implicit appContext: AppContext, system: ActorSystem)
    extends StrictLogging {

  def getZKNodeTreeAsJson(path: String): String =
    try {
      val block = _getZKNodeTreeAsJson(path)
      if (block == "") "{}" else block
    } catch {
      case _ => "{}"
    }

  def _getZKNodeTreeAsJson(path: String, indent: Boolean = true, depth: Int = 1): String = {
    val subNodes = zkManager.getChildren(path)
    var block    = ""
    val prevPad  = if (indent) "".padTo((depth - 1) * 2, ' ') else ""
    val pad      = if (indent) "".padTo(depth * 2, ' ') else ""
    val newLine  = if (indent) "\n" else ""

    if (subNodes.isDefined && subNodes.get.nonEmpty) {
      block += "{" + newLine
      subNodes.foreach(nodeList => {
        for (i <- nodeList.indices) {
          val nodename = nodeList(i)
          block += pad + s""""$nodename": """ +
            _getZKNodeTreeAsJson(s"$path/$nodename", indent, depth + 1)
          if (i < nodeList.length - 1)
            block += ","
          block += newLine
        }
      })
      if (depth == 1)
        block += "}" + newLine
      else
        block += prevPad + "}"
    } else {
      if (depth == 1) {
        block = zkManager.getData(path).getOrElse("")
      } else {
        block = "\"" + zkManager
          .getData(path)
          .getOrElse("")
          .replaceAll("\n|\r", "")
          .replaceAll("""\\""", """\\\\""")
          .replaceAll("\"", "\\\\\"") + "\""
      }
    }

    block
  }

  def updateZKNodeTreeByJson(path: String, json: String): Unit = {
    zkManager.ensurePath(path)
    logger.info(s"ensure path $path")

    import com.typesafe.config.ConfigValueType._
    def update(parentPath: String, co: ConfigObject): Unit =
      co.entrySet()
        .asScala
        .foreach(entry => {
          val key      = entry.getKey
          val currPath = s"$parentPath/$key"
          val currType = entry.getValue.valueType()

          currType match {
            case OBJECT =>
              zkManager.ensurePath(currPath)
              logger.info(s"ensure path $currPath")
              update(currPath, entry.getValue.asInstanceOf[ConfigObject])
            case LIST =>
            case NULL | STRING | BOOLEAN | NUMBER =>
              val currValue = if (currType == NULL) "" else entry.getValue.unwrapped().toString
              zkManager.ensurePath(currPath, currValue)
              logger.info(s"ensure path $currPath with value $currValue")
          }
        })

    val root = ConfigFactory.parseString(json).root()
    update(path, root)
  }

  def createResponse(code: Int, errorMessage: String = ""): String = {
    val message = errorMessage.replaceAll("""\\""", """\\\\""").replaceAll("\"", "\\\\\"")
    s"""{"code": "$code", "message": "$message"}"""
  }

  def wrapPath(path: Option[String]): String =
    if (path.isEmpty || path.get.isEmpty)
      appContext.getAppEnv()
    else
      s"${appContext.getAppEnv()}/${path.get}"

  // -------- Actions

  def getZKTree(path: Option[String]): String =
    getZKNodeTreeAsJson(wrapPath(path))

  def updateZKTree(path: Option[String], json: String): String =
    try {
      updateZKNodeTreeByJson(wrapPath(path), json)
      createResponse(0)
    } catch {
      case ex: Throwable => createResponse(1, ex.getMessage)
    }

  def getStories(): String =
    getZKNodeTreeAsJson(wrapPath(Some("stories")))

  def getStory(storyName: String): String =
    getZKNodeTreeAsJson(wrapPath(Some(s"stories/$storyName")))

  def createStory(storyInfo: StoryInfo): String =
    try {
      if (storyInfo.source.isEmpty || storyInfo.sink.isEmpty) {
        throw new IllegalArgumentException("Source and Sink settings are required for creating Story.")
      }

      val storyPath = wrapPath(Some(s"stories/${storyInfo.name}"))
      zkManager.ensurePath(s"$storyPath/status", storyInfo.status.getOrElse("INIT"))
      zkManager.ensurePath(s"$storyPath/source", storyInfo.source.get)
      zkManager.ensurePath(s"$storyPath/sink", storyInfo.sink.get)
      storyInfo.transforms.foreach(d => zkManager.ensurePath(s"$storyPath/transforms", d))
      storyInfo.fallback.foreach(d => zkManager.ensurePath(s"$storyPath/fallback", d))

      createResponse(0)
    } catch {
      case ex: Throwable => createResponse(1, ex.getMessage)
    }

  def updateStory(storyInfo: StoryInfo): String =
    try {
      val storyPath = wrapPath(Some(s"stories/${storyInfo.name}"))

      storyInfo.source.foreach(d => zkManager.setData(s"$storyPath/source", d))
      storyInfo.sink.foreach(d => zkManager.setData(s"$storyPath/sink", d))
      storyInfo.transforms.foreach(d => zkManager.setData(s"$storyPath/transforms", d))
      storyInfo.fallback.foreach(d => zkManager.setData(s"$storyPath/fallback", d))

      createResponse(0)
    } catch {
      case ex: Throwable => createResponse(1, ex.getMessage)
    }

  def getRunners(): String =
    getZKNodeTreeAsJson(wrapPath(Some("runners")))

  def getRunner(runnerName: String): String =
    getZKNodeTreeAsJson(wrapPath(Some(s"runners/$runnerName")))

  def assignStory(runnerName: String, storyName: String): String =
    try {
      zkManager.ensurePath(wrapPath(Some(s"runners/$runnerName/stories/$storyName")))
      createResponse(0)
    } catch {
      case ex: Throwable => createResponse(1, ex.getMessage)
    }

  def unassignStory(runnerName: String, storyName: String): String =
    try {
      zkManager.deletePath(wrapPath(Some(s"runners/$runnerName/stories/$storyName")))
      createResponse(0)
    } catch {
      case ex: Throwable => createResponse(1, ex.getMessage)
    }

  def getTopics(): String =
    getZKNodeTreeAsJson(wrapPath(Some("topics")))

  def updateTopics(topics: String): String =
    try {
      zkManager.setData(wrapPath(Some("topics")), topics)
      createResponse(0)
    } catch {
      case ex: Throwable => createResponse(1, ex.getMessage)
    }

}
