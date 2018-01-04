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

package com.thenetcircle.event_bus.connectors.http

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.settings.ServerSettings
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream._
import akka.stream.scaladsl.{Flow, GraphDSL, Source}
import akka.stream.stage._
import com.thenetcircle.event_bus.event.extractor.{ExtractedData, ExtractorFactory, IExtractor}
import com.thenetcircle.event_bus.event.Event
import com.thenetcircle.event_bus.event.extractor.DataFormat.DataFormat
import com.thenetcircle.event_bus.interface.{PlotBuilder, SourcePlot}
import com.thenetcircle.event_bus.tracing.Tracing
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import net.ceedubs.ficus.Ficus._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

case class HttpSourceSettings(interface: String,
                              port: Int,
                              format: DataFormat,
                              maxConnections: Int,
                              perConnectionParallelism: Int,
                              serverSettingsOption: Option[ServerSettings] = None)

class HttpSource(
    settings: HttpSourceSettings,
    overriddenHttpBind: Option[Source[Flow[HttpResponse, HttpRequest, Any], _]] = None
)(implicit system: ActorSystem, materializer: Materializer, executor: ExecutionContext)
    extends SourcePlot
    with StrictLogging {

  import HttpSource._

  implicit val extractor: IExtractor = ExtractorFactory.getExtractor(settings.format)

  private val httpBind: Source[Flow[HttpResponse, HttpRequest, Any], _] =
    overriddenHttpBind.getOrElse(settings.serverSettingsOption match {
      case Some(ss) =>
        Http()
          .bind(interface = settings.interface, port = settings.port, settings = ss)
          .map(_.flow)
      case None =>
        Http()
          .bind(interface = settings.interface, port = settings.port)
          .map(_.flow)
    })

  override def getGraph(): Source[Event, NotUsed] =
    httpBind
      .flatMapMerge(
        settings.maxConnections,
        httpBindFlow => {
          Source.fromGraph(GraphDSL.create() {
            implicit builder =>
              import GraphDSL.Implicits._

              val client = builder.add(httpBindFlow)
              val requestHandler = builder.add(new HttpRequestHandler())
              val responseHolder =
                Flow[Future[HttpResponse]]
                  .mapAsync(settings.perConnectionParallelism)(identity)

              /** ----- work flow ----- */
              // since Http().bind using join, The direction is a bit different
              // format: off

              client ~> requestHandler.in

                        requestHandler.out0 ~> responseHolder ~> client

              // format: on

              SourceShape(requestHandler.out1)
          })
        }
      )
      .mapMaterializedValue(m => NotUsed)

  override def getCommittingGraph(): Flow[Event, Event, NotUsed] =
    Flow[Event].map(event => {
      event
        .getContext[Promise[HttpResponse]]("responsePromise")
        .foreach(p => p.success(successfulResponse))
      event
    })
}

object HttpSource {

  val failedResponse =
    HttpResponse(StatusCodes.BadRequest, entity = HttpEntity("ko"))
  val successfulResponse = HttpResponse(entity = HttpEntity("ok"))

  /** Does transform a incoming [[HttpRequest]] to a [[Future]] of [[HttpResponse]]
    * and a [[Event]] with a committer to complete the [[Future]]
    *
    * {{{
    *                             +------------+
    *            In[HttpRequest] ~~>           |
    *                             |           ~~> Out1[Event]
    * Out0[Future[HttpResponse]] <~~           |
    *                             +------------+
    * }}}
    *
    * '''Emits when'''
    * a incoming [[HttpRequest]] successful transformed to a [[Event]],
    * the Future of [[HttpResponse]] will always emit to out0
    *
    * '''Backpressures when''' any of the outputs backpressure
    *
    * '''Completes when''' upstream completes
    *
    * '''Cancels when''' when any downstreams cancel
    */
  final class HttpRequestHandler()(implicit executor: ExecutionContext, extractor: IExtractor)
      extends GraphStage[FanOutShape2[HttpRequest, Future[HttpResponse], Event]] {

    val unmarshaller: Unmarshaller[HttpEntity, ExtractedData] =
      Unmarshaller.byteStringUnmarshaller.andThen(
        Unmarshaller.apply(_ => data => extractor.extract(data))
      )

    val in: Inlet[HttpRequest] = Inlet("inlet-http-request")
    val out0: Outlet[Future[HttpResponse]] = Outlet("outlet-http-response")
    val out1: Outlet[Event] = Outlet("outlet-event")

    override def shape: FanOutShape2[HttpRequest, Future[HttpResponse], Event] =
      new FanOutShape2(in, out0, out1)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) with StageLogging with Tracing {

        def tryPullIn(): Unit =
          if (!hasBeenPulled(in) && isAvailable(out0) && isAvailable(out1)) {
            log.debug("tryPull in")
            tryPull(in)
          }

        setHandler(in, new InHandler {
          override def onPush(): Unit = {
            log.debug("onPush in -> push out0")
            push(out0, requestPreprocess(grab(in)))
          }
        })

        setHandler(out0, new OutHandler {
          override def onPull(): Unit = {
            log.debug("onPull out0")
            tryPullIn()
          }
        })

        setHandler(out1, new OutHandler {
          override def onPull(): Unit = {
            log.debug("onPull out1")
            tryPullIn()
          }
        })

        def requestPreprocess(request: HttpRequest): Future[HttpResponse] = {
          val responsePromise = Promise[HttpResponse]
          val extractedDataFuture =
            unmarshaller.apply(request.entity)(executor, materializer)
          val callback = getEventExtractingCallback(responsePromise)
          extractedDataFuture.onComplete(result => callback.invoke(result))

          responsePromise.future
        }

        def getEventExtractingCallback(
            responsePromise: Promise[HttpResponse]
        ): AsyncCallback[Try[ExtractedData]] =
          getAsyncCallback[Try[ExtractedData]] {
            case Success(extractedData) =>
              val event = Event(
                metadata = extractedData.metadata,
                body = extractedData.body,
                context = Map("responsePromise" -> responsePromise)
              )
              log.debug("push out1")
              push(out1, event)

            case Failure(ex) =>
              log.info(s"Request send failed with error: ${ex.getMessage}")
              responsePromise.success(failedResponse)
              tryPullIn()
          }
      }
  }
}

class HttpSourceBuilder()(implicit system: ActorSystem,
                          materializer: Materializer,
                          executor: ExecutionContext)
    extends PlotBuilder
    with StrictLogging {

  override def buildFromConfig(config: Config): HttpSource = {

    val defaultConfig: Config = ConfigFactory.parseString(
      """ 
        |{
        |  # interface = ...
        |  # port = ...
        |  format = default
        |  max-connections = 1000
        |  pre-connection-parallelism = 10
        |  # akka.http.server {} // override "akka.http.server" default settings
        |}
      """.stripMargin
    )

    try {
      val mergedConfig: Config = config.withFallback(defaultConfig)

      val serverSettingsOption: Option[ServerSettings] =
        if (mergedConfig.hasPath("akka.http.server"))
          Some(ServerSettings(mergedConfig.withFallback(system.settings.config)))
        else None

      new HttpSource(
        HttpSourceSettings(
          mergedConfig.as[String]("interface"),
          mergedConfig.as[Int]("port"),
          mergedConfig.as[DataFormat]("format"),
          mergedConfig.as[Int]("max-connections"),
          mergedConfig.as[Int]("pre-connection-parallelism"),
          serverSettingsOption
        )
      )

    } catch {
      case ex: Throwable =>
        logger.error(s"Creating a HttpSource failed with error: ${ex.getMessage}")
        throw ex
    }
  }
}
