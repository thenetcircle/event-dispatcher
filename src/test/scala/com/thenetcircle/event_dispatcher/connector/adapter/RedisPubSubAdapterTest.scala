package com.thenetcircle.event_dispatcher.connector.adapter

import akka.util.ByteString
import com.thenetcircle.event_dispatcher.stage.redis.{ IncomingMessage, OutgoingMessage }
import com.thenetcircle.event_dispatcher.{ RawEvent, TestCase }

class RedisPubSubAdapterTest extends TestCase {

  val rawEvent = RawEvent(
    ByteString("test-data"),
    Map(
      "patternMatched" -> Some("test-*")
    ),
    Some("test-channel")
  )

  test("reid pub-sub source adapter") {

    val adapter = RedisPubSubSourceAdapter
    val message =
      IncomingMessage("test-channel", ByteString("test-data"), Some("test-*"))

    adapter.fit(message) shouldEqual rawEvent

  }

  test("redis pub-sub sink adapter") {

    val adapter = RedisPubSubSinkAdapter
    adapter.unfit(rawEvent) shouldEqual OutgoingMessage[ByteString](
      "test-channel",
      ByteString("test-data")
    )

  }

}
