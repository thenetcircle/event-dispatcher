package com.thenetcircle.event_dispatcher.driver.extractor

import java.text.SimpleDateFormat

import akka.util.ByteString
import com.thenetcircle.event_dispatcher.EventFmt
import com.thenetcircle.event_dispatcher.{ BizData, Event, RawEvent, TestCase }

class ActivityStreamsExtractorTest extends TestCase {

  val json =
    """
      |{
      |  "id": "user-1008646-1500290771-820",
      |  "verb": "user.login",
      |  "provider": {
      |    "id": "COMM1",
      |    "objectType": "community"
      |  },
      |  "actor": {
      |    "id": "1008646",
      |    "objectType": "user"
      |  },
      |  "published": "2017-07-17T13:26:11+02:00",
      |  "context": {
      |    "ip": "79.198.111.108",
      |    "user-agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 9_3_5 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13G36 Safari/601.1",
      |    "hasMembership": "0",
      |    "membership": 1
      |  },
      |  "version": "1.0",
      |  "extra": {
      |    "name": "user.login",
      |    "group": "user_1008646",
      |    "mode": "sync_plus",
      |    "propagationStopped": false,
      |    "class": "dfEvent_User"
      |  }
      |}
    """.stripMargin

  test("json parser") {
    import spray.json._
    import ActivityStreamsProtocol._

    val jsonAst = json.parseJson
    val activity = jsonAst.convertTo[FatActivity]

    activity.actor.id.get shouldEqual "1008646"
    activity.id.get shouldEqual "user-1008646-1500290771-820"
    activity.verb.get shouldEqual "user.login"

    val context = activity.context.get
    context("ip") shouldEqual JsString("79.198.111.108")

    val extra = activity.extra.get
    extra("name") shouldEqual JsString("user.login")
    extra("propagationStopped") shouldEqual JsBoolean(false)
  }

  test("extrator") {
    val extractor = new ActivityStreamsExtractor
    val rawEvent = RawEvent(
      body = ByteString(json),
      context = Map.empty[String, Any],
      channel = None
    )

    val event = extractor.extract(rawEvent)
    val expectedEvent = Event(
      uuid = "test-uuid",
      timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
        .parse("2017-07-17T13:26:11+02:00")
        .getTime,
      rawEvent = rawEvent,
      bizData = BizData(
        sessionId = Some("user-1008646-1500290771-820"),
        provider = Some("COMM1"),
        category = Some("user.login"),
        actorId = Some("1008646"),
        actorType = Some("user")
      ),
      format = EventFmt.ActivityStreams()
    )

    event.timestamp shouldEqual expectedEvent.timestamp
    event.rawEvent shouldEqual expectedEvent.rawEvent
    event.bizData shouldEqual expectedEvent.bizData
  }

}