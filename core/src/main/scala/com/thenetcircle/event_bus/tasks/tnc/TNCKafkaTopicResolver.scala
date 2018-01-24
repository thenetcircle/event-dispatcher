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

package com.thenetcircle.event_bus.tasks.tnc

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.thenetcircle.event_bus.context.{TaskBuildingContext, TaskRunningContext}
import com.thenetcircle.event_bus.interfaces.EventStatus.{Fail, Norm}
import com.thenetcircle.event_bus.interfaces.{
  Event,
  EventStatus,
  TransformTask,
  TransformTaskBuilder
}
import com.thenetcircle.event_bus.misc.{Util, ZKManager}
import com.typesafe.scalalogging.StrictLogging
import org.apache.curator.framework.recipes.cache.{ChildData, PathChildrenCache}
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

class TNCKafkaTopicResolver(zkManager: ZKManager,
                            val defaultTopic: String,
                            val useCache: Boolean = false)
    extends TransformTask
    with StrictLogging {

  private var inited: Boolean = false
  private var index: Map[String, String] = Map.empty
  private val cached: ConcurrentHashMap[String, String] = new ConcurrentHashMap()
  private var zkWatcher: Option[PathChildrenCache] = None

  def init(): Unit = if (!inited) {
    updateAndWatchMapping()
    inited = true
  }

  val zkInited = new AtomicBoolean(false)
  private def updateAndWatchMapping(): Unit = {
    zkManager.ensurePath("topics")
    zkWatcher = Some(
      zkManager.watchChildren("topics", startMode = StartMode.POST_INITIALIZED_EVENT) { (et, wc) =>
        if (et.getType == INITIALIZED ||
            (et.getType == CHILD_ADDED && zkInited.get()) ||
            et.getType == CHILD_UPDATED ||
            et.getType == CHILD_REMOVED) {
          if (et.getType == INITIALIZED) zkInited.compareAndSet(false, true)
          val mapping = createMappingFromZKData(wc.getCurrentData.asScala.toList)
          logger.info(s"get new mapping from zookeeper $mapping")
          if (mapping.nonEmpty)
            updateMapping(mapping)
        }
      }
    )
  }

  def createMappingFromZKData(data: List[ChildData]): Map[String, String] = {
    data
      .map(child => Util.getLastPartOfPath(child.getPath) -> Util.makeUTF8String(child.getData))
      .toMap
  }

  def getIndex(): Map[String, String] = synchronized { index }
  def updateIndex(_index: Map[String, String]): Unit = synchronized {
    logger.info(s"updating new index ${_index}")
    index = _index
  }
  def updateMapping(_mapping: Map[String, String]): Unit = {
    updateIndex(_mapping.map(v => v._2 -> v._1))
    if (useCache) cached.clear()
  }

  override def prepare()(
      implicit runningContext: TaskRunningContext
  ): Flow[Event, (EventStatus, Event), NotUsed] = {
    init()
    Flow[Event].map(event => {
      Try(resolveEvent(event)) match {
        case Success(newEvent) =>
          logger.debug(s"new resolved event $newEvent")
          (Norm, newEvent)
        case Failure(ex) =>
          logger.error(s"resolve topic failed with error $ex")
          (Fail(ex), event)
      }
    })
  }

  def getTopicFromIndex(eventName: String): Option[String] = {
    getIndex()
      .find {
        case (pattern, _) =>
          eventName matches pattern
      }
      .map(_._2)
  }

  // TODO: performance test
  def resolveEvent(event: Event): Event = {
    if (event.metadata.group.isDefined) return event
    if (event.metadata.name.isEmpty) return event.withGroup(defaultTopic)

    val eventName = event.metadata.name.get
    var topic = ""
    if (useCache) {
      val cachedTopic = cached.get(eventName)
      if (cachedTopic != null) {
        topic = cachedTopic
      } else {
        topic = getTopicFromIndex(eventName).getOrElse(defaultTopic)
        cached.put(eventName, topic)
      }
    } else {
      topic = getTopicFromIndex(eventName).getOrElse(defaultTopic)
    }

    return event.withGroup(topic)
  }

  override def shutdown()(implicit runningContext: TaskRunningContext): Unit = {
    index = Map.empty
    cached.clear()
    zkWatcher.foreach(_.close())
  }
}

class TNCKafkaTopicResolverBuilder() extends TransformTaskBuilder {

  override def build(
      configString: String
  )(implicit buildingContext: TaskBuildingContext): TNCKafkaTopicResolver = {
    val config = Util
      .convertJsonStringToConfig(configString)
      .withFallback(buildingContext.getSystemConfig().getConfig("task.tnc-topic-resolver"))

    val zkManger = ZKManager.getInstance()

    new TNCKafkaTopicResolver(
      zkManger,
      config.getString("default-topic"),
      config.getBoolean("use-cache")
    )
  }

}