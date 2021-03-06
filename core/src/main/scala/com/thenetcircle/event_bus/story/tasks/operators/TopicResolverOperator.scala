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

package com.thenetcircle.event_bus.story.tasks.operators

import akka.stream.scaladsl.Flow
import com.thenetcircle.event_bus.AppContext
import com.thenetcircle.event_bus.event.Event
import com.thenetcircle.event_bus.event.EventStatus.{FAILED, NORMAL}
import com.thenetcircle.event_bus.misc.ZKManager
import com.thenetcircle.event_bus.story.interfaces.{ITaskBuilder, ITaskLogging, IUndiOperator}
import com.thenetcircle.event_bus.story.tasks.kafka.replaceKafkaTopicSubstitutes
import com.thenetcircle.event_bus.story.{Payload, StoryMat, TaskRunningContext}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.curator.framework.recipes.cache.NodeCache
import spray.json._

import scala.collection.mutable.ArrayBuffer
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

case class TopicInfo(topic: String, patterns: Option[List[String]], channels: Option[List[String]])

object TopicInfoProtocol extends DefaultJsonProtocol {
  implicit val topicInfoFormat = jsonFormat3(TopicInfo)
}

class TopicResolverOperator(zkManager: ZKManager, val _defaultTopic: String) extends IUndiOperator with ITaskLogging {

  import TopicInfoProtocol._

  private var inited: Boolean              = false
  private var defaultTopic: String         = _defaultTopic
  private var zkWatcher: Option[NodeCache] = None

  private var nameIndex: Map[String, String]    = Map.empty
  private var channelIndex: Map[String, String] = Map.empty

  def init()(
      implicit runningContext: TaskRunningContext
  ): Unit = if (!inited) {
    defaultTopic = replaceKafkaTopicSubstitutes(defaultTopic)
    updateAndWatchIndex()
    inited = true
  }

  private def updateAndWatchIndex()(
      implicit runningContext: TaskRunningContext
  ): Unit = {
    zkManager.ensurePath("topics")
    zkWatcher = Some(
      zkManager.watchData("topics") {
        _ foreach {
          data =>
            if (data.nonEmpty) {
              try {
                val topicInfo        = data.parseJson.convertTo[List[TopicInfo]]
                val nameIndexList    = ArrayBuffer.empty[(String, String)]
                val channelIndexList = ArrayBuffer.empty[(String, String)]

                topicInfo
                  .foreach(info => {
                    // val _topic = replaceSubstitutes(info.topic)
                    val _topic = info.topic
                    info.patterns.foreach(_.foreach(s => {
                      nameIndexList += (s -> _topic)
                    }))
                    info.channels.foreach(_.foreach(s => {
                      channelIndexList += (s -> _topic)
                    }))
                  })

                updateIndex(nameIndexList.toMap, channelIndexList.toMap)
              } catch {
                case NonFatal(ex) =>
                  taskLogger.error(s"Updating TopicResolver mapping failed with error: $ex")
              }
            }
        }
      }
    )
  }

  def updateIndex(_nameIndex: Map[String, String], _channelIndex: Map[String, String]): Unit = {
    taskLogger.info(
      s"Updating TopicResolver mapping, nameIndex : " + _nameIndex + ", channelIndex: " + _channelIndex
    )
    nameIndex = _nameIndex
    channelIndex = _channelIndex
  }

  def getNameIndex(): Map[String, String] = nameIndex

  def getChannelIndex(): Map[String, String] = channelIndex

  override def flow()(
      implicit runningContext: TaskRunningContext
  ): Flow[Payload, Payload, StoryMat] = {
    init()
    Flow[Payload].map {
      case (NORMAL, event) =>
        Try(resolveEvent(event)) match {
          case Success(newEvent) =>
            (NORMAL, newEvent)
          case Failure(ex) =>
            taskLogger.error(s"Resolve topic of a event ${event.summary} failed with error $ex")
            (FAILED(ex, getTaskName()), event)
        }
      case others => others
    }
  }

  def getTopicFromIndex(event: Event): Option[String] = {
    var result: Option[String] = None

    if (event.metadata.channel.isDefined) {
      result = getChannelIndex()
        .find {
          case (pattern, _) =>
            event.metadata.channel.get matches pattern
        }
        .map(_._2)
    }
    if (result.isEmpty && event.metadata.name.isDefined) {
      result = getNameIndex()
        .find {
          case (pattern, _) =>
            event.metadata.name.get matches pattern
        }
        .map(_._2)
    }

    result
  }

  def resolveEvent(event: Event): Event = {
    if (event.metadata.topic.isDefined) {
      taskLogger.info(
        s"The event ${event.summary} has topic ${event.metadata.topic.get} already, will not resolve it."
      )
      return event
    }
    if (event.metadata.name.isEmpty && event.metadata.channel.isEmpty) {
      taskLogger.info(
        s"The event ${event.summary} has no name and channel, will be send to default topic $defaultTopic."
      )
      return event.withTopic(defaultTopic)
    }

    val newTopic = getTopicFromIndex(event).getOrElse(defaultTopic)
    taskLogger.info(s"The event ${event.summary} has been resolved to new topic $newTopic")

    event.withTopic(newTopic)
  }

  override def shutdown()(implicit runningContext: TaskRunningContext): Unit = {
    taskLogger.info(s"Shutting down TopicResolver Operator.")
    nameIndex = Map.empty
    channelIndex = Map.empty
    zkWatcher.foreach(_.close())
  }
}

class TopicResolverOperatorBuilder() extends ITaskBuilder[TopicResolverOperator] {

  override val taskType: String = "tnc-topic-resolver"

  override val defaultConfig: Config =
    ConfigFactory.parseString(
      """{
        |  default-topic = "event-v2-{app_name}{app_env}-default"
        |}""".stripMargin
    )

  override def buildTask(
      config: Config
  )(implicit appContext: AppContext): TopicResolverOperator = {
    val zkMangerOption = appContext.getZKManager()
    if (zkMangerOption.isEmpty) {
      throw new IllegalArgumentException("ZooKeeperManager is required for TopicResolverOperator")
    }

    new TopicResolverOperator(
      zkMangerOption.get,
      config.getString("default-topic")
    )
  }

}
