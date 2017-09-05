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

package com.thenetcircle.event_bus.dispatcher

import akka.actor.ActorSystem
import akka.stream.scaladsl.{RunnableGraph, Sink}
import akka.stream.{ActorMaterializer, Materializer}
import com.thenetcircle.event_bus.dispatcher.endpoint.EndPoint
import com.thenetcircle.event_bus.pipeline.RightPort
import com.thenetcircle.event_bus.pipeline.kafka.KafkaPipelineFactory

class Dispatcher(settings: DispatcherSettings,
                 pipelineRightPort: RightPort,
                 endPoint: EndPoint)(implicit materializer: Materializer) {

  // TODO: draw a graph in comments
  // TODO: error handler
  // TODO: parallel and async
  // TODO: Mat value
  lazy val dispatchStream: RunnableGraph[_] =
    pipelineRightPort.port
      .flatMapMerge(settings.maxParallelSources,
                    source => source.via(endPoint.port.async))
      .via(pipelineRightPort.committer.async)
      .to(Sink.ignore)

  // TODO add a transporter controller as a materialized value
  def run(): Unit = dispatchStream.run()

}

object Dispatcher {
  def apply(settings: DispatcherSettings)(
      implicit system: ActorSystem): Dispatcher = {

    implicit val materializer =
      ActorMaterializer(settings.materializerSettings, Some(settings.name))

    val pipelineFactory = KafkaPipelineFactory
    val pipelineRightPort = {
      val _rightPortSettings =
        pipelineFactory.getRightPortSettings(settings.pipelineRightPortConfig)
      pipelineFactory.getRightPort(settings.pipelineName, _rightPortSettings) match {
        case Some(p) => p
        case None =>
          throw new IllegalArgumentException(
            s"There is not RightPort of ${settings.pipelineName} found according to the configuration ${_rightPortSettings}")
      }
    }

    val endPoint = EndPoint(settings.endPointSettings)

    new Dispatcher(settings, pipelineRightPort, endPoint)
  }
}
