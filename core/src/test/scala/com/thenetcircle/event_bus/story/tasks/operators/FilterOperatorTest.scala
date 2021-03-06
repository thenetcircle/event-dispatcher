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

import com.thenetcircle.event_bus.TestBase
import com.thenetcircle.event_bus.event.DefaultEventImpl
import com.thenetcircle.event_bus.event.extractor.DataFormat
import com.thenetcircle.event_bus.event.{EventBody, EventMetaData, EventTransportMode}
import com.thenetcircle.event_bus.event.EventStatus.{NORMAL, SKIPPING}

class FilterOperatorTest extends TestBase {

  behavior of "FilterOperator"

  val builder = new FilterOperatorBuilder

  /*val eventFilter = builder.build("""{
        |"event-name-white-list": ["user\\..*", "wio\\..*"],
        |"event-name-black-list": ["image\\..*"],
        |"channel-white-list": ["membership", "forum"],
        |"channel-black-list": ["quick\\-.*"],
        |"allowed-transport-modes": ["sync-plus", "sync", "both"],
        |"only-extras": {
        |   "actorId": "1234",
        |   "generatorId": "tnc-event-dispatcher"
        |}
        |}""".stripMargin)*/

  it should "testEventNameWhiteList" in {
    val eventFilter = storyBuilder.buildTaskWithBuilder("""{
        |"event-name-white-list": ["user\\..*", "wio\\..*"]
        |}""".stripMargin)(builder)

    var testEvent = createTestEvent("user.login")
    eventFilter.checkEvent(testEvent) shouldEqual (NORMAL, testEvent)

    testEvent = createTestEvent("wio.invisible")
    eventFilter.checkEvent(testEvent) shouldEqual (NORMAL, testEvent)

    testEvent = createTestEvent("membership.login")
    eventFilter.checkEvent(testEvent) shouldEqual (SKIPPING, testEvent)

    testEvent = DefaultEventImpl(
      uuid = "TestEvent-" + java.util.UUID.randomUUID().toString,
      metadata = EventMetaData(),
      body = EventBody("", DataFormat.UNKNOWN)
    )
    eventFilter.checkEvent(testEvent) shouldEqual (SKIPPING, testEvent)
  }

  it should "testEventNameBlackList" in {
    val eventFilter = storyBuilder.buildTaskWithBuilder("""{
        |"event-name-black-list": ["user\\..*", "wio\\..*"]
        |}""".stripMargin)(builder)

    var testEvent = createTestEvent("user.login")
    eventFilter.checkEvent(testEvent) shouldEqual (SKIPPING, testEvent)

    testEvent = createTestEvent("wio.invisible")
    eventFilter.checkEvent(testEvent) shouldEqual (SKIPPING, testEvent)

    testEvent = createTestEvent("membership.login")
    eventFilter.checkEvent(testEvent) shouldEqual (NORMAL, testEvent)

    testEvent = DefaultEventImpl(
      uuid = "TestEvent-" + java.util.UUID.randomUUID().toString,
      metadata = EventMetaData(),
      body = EventBody("", DataFormat.UNKNOWN)
    )
    eventFilter.checkEvent(testEvent) shouldEqual (NORMAL, testEvent)
  }

  it should "testEventChannelWhiteList" in {
    val eventFilter = storyBuilder.buildTaskWithBuilder("""{
        |"channel-white-list": ["membership", "forum"]
        |}""".stripMargin)(builder)

    var testEvent = createTestEvent(channel = None)
    eventFilter.checkEvent(testEvent) shouldEqual (SKIPPING, testEvent)

    testEvent = createTestEvent(channel = Some("mychannel"))
    eventFilter.checkEvent(testEvent) shouldEqual (SKIPPING, testEvent)

    testEvent = createTestEvent(channel = Some("membership"))
    eventFilter.checkEvent(testEvent) shouldEqual (NORMAL, testEvent)

    testEvent = createTestEvent(channel = Some("abc.forum.def"))
    eventFilter.checkEvent(testEvent) shouldEqual (SKIPPING, testEvent)

    testEvent = createTestEvent(channel = Some("forum"))
    eventFilter.checkEvent(testEvent) shouldEqual (NORMAL, testEvent)
  }

  it should "testEventChannelBlackList" in {
    val eventFilter = storyBuilder.buildTaskWithBuilder("""{
        |"channel-black-list": ["membership", "forum"]
        |}""".stripMargin)(builder)

    var testEvent = createTestEvent(channel = None)
    eventFilter.checkEvent(testEvent) shouldEqual (NORMAL, testEvent)

    testEvent = createTestEvent(channel = Some("mychannel"))
    eventFilter.checkEvent(testEvent) shouldEqual (NORMAL, testEvent)

    testEvent = createTestEvent(channel = Some("membership"))
    eventFilter.checkEvent(testEvent) shouldEqual (SKIPPING, testEvent)

    testEvent = createTestEvent(channel = Some("abc.forum.def"))
    eventFilter.checkEvent(testEvent) shouldEqual (NORMAL, testEvent)

    testEvent = createTestEvent(channel = Some("forum"))
    eventFilter.checkEvent(testEvent) shouldEqual (SKIPPING, testEvent)
  }

  it should "testEventTransportMode" in {
    val eventFilter = storyBuilder.buildTaskWithBuilder("""{
        |"allowed-transport-modes": ["ASYNC", "BOTH", "NONE", "SYNC"]
        |}""".stripMargin)(builder)

    var testEvent = createTestEvent()
    eventFilter.checkEvent(testEvent) shouldEqual (NORMAL, testEvent)

    testEvent = createTestEvent(transportMode = Some(EventTransportMode.SYNC_PLUS))
    eventFilter.checkEvent(testEvent) shouldEqual (SKIPPING, testEvent)

    testEvent = createTestEvent(transportMode = Some(EventTransportMode.ASYNC))
    eventFilter.checkEvent(testEvent) shouldEqual (NORMAL, testEvent)

    testEvent = createTestEvent(transportMode = Some(EventTransportMode.BOTH))
    eventFilter.checkEvent(testEvent) shouldEqual (NORMAL, testEvent)

    testEvent = createTestEvent(transportMode = Some(EventTransportMode.OTHERS("SYNC")))
    eventFilter.checkEvent(testEvent) shouldEqual (NORMAL, testEvent)

    testEvent = createTestEvent(transportMode = Some(EventTransportMode.OTHERS("UNKNOWN")))
    eventFilter.checkEvent(testEvent) shouldEqual (SKIPPING, testEvent)
  }

  it should "testEventExtras" in {
    val eventFilter = storyBuilder.buildTaskWithBuilder("""{
        |"only-extras": {
        |   "actorId": "1234",
        |   "generatorId": "tnc-event-dispatcher"
        |}
        |}""".stripMargin)(builder)

    var testEvent = createTestEvent()
    eventFilter.checkEvent(testEvent) shouldEqual (SKIPPING, testEvent)

    testEvent = createTestEvent(extra = Map("actorId" -> "1234"))
    eventFilter.checkEvent(testEvent) shouldEqual (SKIPPING, testEvent)

    testEvent = createTestEvent(extra = Map("generatorId" -> "tnc-event-dispatcher"))
    eventFilter.checkEvent(testEvent) shouldEqual (SKIPPING, testEvent)

    testEvent =
      createTestEvent(extra = Map("actorId" -> "1234", "objectId" -> "4321", "generatorId" -> "tnc-event-dispatcher"))
    eventFilter.checkEvent(testEvent) shouldEqual (NORMAL, testEvent)
  }

}
