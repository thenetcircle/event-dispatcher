package com.thenetcircle.event_bus.driver.adapter

import akka.util.ByteString
import com.thenetcircle.event_bus.alpakka.redis.{ IncomingMessage, OutgoingMessage }
import com.thenetcircle.event_bus.{ EventSource, RawEvent }

object RedisPubSubSourceAdapter extends SourceAdapter[IncomingMessage] {
  def fit(message: IncomingMessage): RawEvent =
    RawEvent(message.data,
             message.channel,
             Map(
               "patternMatched" -> message.patternMatched
             ),
             EventSource.Redis)
}

object RedisPubSubSinkAdapter extends SinkAdapter[OutgoingMessage[ByteString]] {
  override def unfit(event: RawEvent): OutgoingMessage[ByteString] =
    OutgoingMessage[ByteString](event.channel, event.body)
}
