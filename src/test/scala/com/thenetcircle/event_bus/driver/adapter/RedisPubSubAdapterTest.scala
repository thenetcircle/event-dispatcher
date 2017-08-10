package com.thenetcircle.event_bus.driver.adapter

import akka.util.ByteString
import com.thenetcircle.event_bus.alpakka.redis.{ IncomingMessage, OutgoingMessage }
import com.thenetcircle.event_bus.{ EventSource, RawEvent, TestCase }

class RedisPubSubAdapterTest extends TestCase {

  val rawEvent = RawEvent(
    ByteString("test-data"),
    "test-channel",
    Map(
      "patternMatched" -> Some("test-*")
    ),
    EventSource.Redis
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
