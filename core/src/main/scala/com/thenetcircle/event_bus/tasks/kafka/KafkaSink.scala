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

package com.thenetcircle.event_bus.tasks.kafka

import java.util
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

import akka.NotUsed
import akka.kafka.ProducerMessage.Message
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.{Flow, GraphDSL, Sink}
import akka.stream.stage._
import akka.stream._
import com.thenetcircle.event_bus.context.{TaskBuildingContext, TaskRunningContext}
import com.thenetcircle.event_bus.interfaces.EventStatus.Norm
import com.thenetcircle.event_bus.interfaces.{Event, EventStatus, SinkTask, SinkTaskBuilder}
import com.thenetcircle.event_bus.misc.Util
import com.thenetcircle.event_bus.tasks.kafka.extended.{EventSerializer, KafkaKey, KafkaKeySerializer, KafkaPartitioner}
import com.typesafe.scalalogging.StrictLogging
import net.ceedubs.ficus.Ficus._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}

import scala.concurrent.duration._

case class KafkaSinkSettings(
    bootstrapServers: String,
    defaultTopic: String = "event-default",
    useEventGroupAsTopic: Boolean = true,
    parallelism: Int = 100,
    closeTimeout: FiniteDuration = 60.seconds,
    useDispatcher: Option[String] = None,
    properties: Map[String, String] = Map.empty,
    useAsyncBuffer: Boolean = false,
    asyncBufferSize: Int = 10000
)

class KafkaSink(val settings: KafkaSinkSettings) extends SinkTask with StrictLogging {

  require(settings.bootstrapServers.nonEmpty, "bootstrap servers is required.")

  logger.info(s"Initializing KafkaSink with settings: $settings")

  def getProducerSettings()(
      implicit runningContext: TaskRunningContext
  ): ProducerSettings[ProducerKey, ProducerValue] = {
    var _producerSettings = ProducerSettings[ProducerKey, ProducerValue](
      runningContext.getActorSystem(),
      new KafkaKeySerializer,
      new EventSerializer
    )

    settings.properties.foreach {
      case (_key, _value) => _producerSettings = _producerSettings.withProperty(_key, _value)
    }

    settings.useDispatcher.foreach(dp => _producerSettings = _producerSettings.withDispatcher(dp))

    val clientId = s"eventbus-${runningContext.getAppContext().getAppName()}"

    _producerSettings
      .withParallelism(settings.parallelism)
      .withCloseTimeout(settings.closeTimeout)
      .withProperty(ProducerConfig.PARTITIONER_CLASS_CONFIG, classOf[KafkaPartitioner].getName)
      .withBootstrapServers(settings.bootstrapServers)
    // .withProperty("client.id", clientId)
  }

  def createMessage(event: Event)(
      implicit runningContext: TaskRunningContext
  ): Message[ProducerKey, ProducerValue, Event] = {
    val record = createProducerRecord(event)
    logger.debug(s"new kafka record $record is created")
    Message(record, event)
  }

  def createProducerRecord(event: Event)(
      implicit runningContext: TaskRunningContext
  ): ProducerRecord[ProducerKey, ProducerValue] = {
    var topic: String =
      if (settings.useEventGroupAsTopic) event.metadata.group.getOrElse(settings.defaultTopic)
      else settings.defaultTopic

    // val timestamp: Long      = event.createdAt.getTime
    val key: ProducerKey     = KafkaKey(event)
    val value: ProducerValue = event

    new ProducerRecord[ProducerKey, ProducerValue](
      topic,
      key,
      value
    )

    /*new ProducerRecord[ProducerKey, ProducerValue](
      topic,
      null,
      timestamp.asInstanceOf[java.lang.Long],
      key,
      value
    )*/
  }

  var kafkaProducer: Option[KafkaProducer[ProducerKey, ProducerValue]] = None

  override def prepare()(
      implicit runningContext: TaskRunningContext
  ): Flow[Event, (EventStatus, Event), NotUsed] = {

    val kafkaSettings = getProducerSettings()

    val _kafkaProducer = kafkaProducer.getOrElse({
      logger.info("creating new kakfa producer")
      kafkaProducer = Some(kafkaSettings.createKafkaProducer())
      kafkaProducer.get
    })

    // TODO issue when send to new topics, check here https://github.com/akka/reactive-kafka/issues/163

    // Note that the flow might be materialized multiple times,
    // like from HttpSource(multiple connections), KafkaSource(multiple topicPartitions)
    // TODO pretect that the stream crashed by sending failure
    // TODO use Producer.flexiFlow
    // TODO optimize logging
    val producingFlow = Flow[Event]
      .map(createMessage)
      .via(Producer.flow(kafkaSettings, _kafkaProducer))
      .map(result => {
        val eventBrief = Util.getBriefOfEvent(result.message.passThrough)
        val kafkaBrief =
          s"topic: ${result.metadata.topic()}, partition: ${result.metadata.partition()}, offset: ${result.metadata
            .offset()}, key: ${Option(result.message.record.key()).map(_.rawData).getOrElse("")}"
        logger.info(s"sending event [$eventBrief] to kafka [$kafkaBrief] succeeded.")

        (Norm, result.message.passThrough)
      })

    if (settings.useAsyncBuffer) {
      logger.debug("wrapping async buffer")
      KafkaSink.wrapAsyncBuffer(settings.asyncBufferSize, producingFlow)
    } else {
      producingFlow
    }
  }

  override def shutdown()(implicit runningContext: TaskRunningContext): Unit = {
    logger.info(s"shutting down kafka-sink of story ${runningContext.getStoryName()}.")
    kafkaProducer.foreach(k => {
      k.close(5, TimeUnit.SECONDS); kafkaProducer = None
    })
  }
}

object KafkaSink {

  def wrapAsyncBuffer(bufferSize: Int, producingFlow: Flow[Event, _, _]): Flow[Event, (EventStatus, Event), NotUsed] =
    Flow
      .fromGraph(
        GraphDSL
          .create() { implicit builder =>
            import GraphDSL.Implicits._
            val buffer = builder.add(new AsyncBuffer(bufferSize))
            buffer.out1 ~> producingFlow ~> Sink.ignore
            FlowShape(buffer.in, buffer.out0)
          }
      )

  class AsyncBuffer(bufferSize: Int) extends GraphStage[FanOutShape2[Event, (EventStatus, Event), Event]] {

    val in   = Inlet[Event]("AsyncBuffer.in")
    val out0 = Outlet[(EventStatus, Event)]("AsyncBuffer.out0")
    val out1 = Outlet[Event]("AsyncBuffer.out1")

    val shape: FanOutShape2[Event, (EventStatus, Event), Event] = new FanOutShape2(in, out0, out1)

    override def createLogic(
        inheritedAttributes: Attributes
    ): GraphStageLogic = new GraphStageLogic(shape) with InHandler with StageLogging {
      private val bufferImpl: util.Queue[Event] = new LinkedBlockingQueue(bufferSize)

      private def handleMissedEvent(event: Event): Unit =
        log.warning(s"[MISSED] ${event.body.data}")

      override def onPush(): Unit = {
        val event = grab(in)

        if (isAvailable(out0)) {
          push(out0, (Norm, event))
        }

        // If out1 is available, then it has been pulled but no dequeued element has been delivered.
        // It means the buffer at this moment is definitely empty,
        // so we just push the current element to out, then pull.
        if (isAvailable(out1)) {
          push(out1, event)
        } else {
          if (!bufferImpl.offer(event)) {
            // buffer is full, record log
            log.warning("A event [" + Util.getBriefOfEvent(event) + "] is dropped since the AsyncBuffer is full.")
            handleMissedEvent(event)
          }
        }

        pull(in)
      }

      override def onUpstreamFinish(): Unit =
        if (bufferImpl.isEmpty) completeStage()

      override def postStop(): Unit =
        while (!bufferImpl.isEmpty) {
          handleMissedEvent(bufferImpl.poll())
        }

      setHandler(in, this)

      // outlet for outside
      setHandler(
        out0,
        new OutHandler {
          override def onPull(): Unit =
            if (!hasBeenPulled(in)) pull(in)

          override def onDownstreamFinish(): Unit =
            if (bufferImpl.isEmpty) completeStage()
        }
      )

      // outlet for kafka producer
      setHandler(
        out1,
        new OutHandler {
          override def onPull(): Unit = {
            if (!bufferImpl.isEmpty) push(out1, bufferImpl.poll())
            if (isClosed(in)) {
              if (bufferImpl.isEmpty) completeStage()
            } else if (!hasBeenPulled(in)) {
              pull(in)
            }
          }
        }
      )
    }

  }

}

class KafkaSinkBuilder() extends SinkTaskBuilder {
  override def build(
      configString: String
  )(implicit buildingContext: TaskBuildingContext): KafkaSink = {
    val config = Util
      .convertJsonStringToConfig(configString)
      .withFallback(buildingContext.getSystemConfig().getConfig("task.kafka-sink"))

    val settings =
      KafkaSinkSettings(
        config.as[String]("bootstrap-servers"),
        config.as[String]("default-topic"),
        config.as[Boolean]("use-event-group-as-topic"),
        config.as[Int]("parallelism"),
        config.as[FiniteDuration]("close-timeout"),
        config.as[Option[String]]("use-dispatcher"),
        config.as[Map[String, String]]("properties"),
        config.as[Boolean]("use-async-buffer"),
        config.as[Int]("async-buffer-size")
      )

    new KafkaSink(settings)
  }
}
