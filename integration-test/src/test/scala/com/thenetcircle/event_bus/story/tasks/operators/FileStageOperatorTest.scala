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

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.thenetcircle.event_bus.IntegrationTestBase
import com.thenetcircle.event_bus.event.EventStatus.{FAILED, NORMAL, SKIPPING, STAGING}
import com.thenetcircle.event_bus.story.Payload

import scala.concurrent.Await
import scala.concurrent.duration._

class FileStageOperatorTest extends IntegrationTestBase {

  behavior of "FileStageOperatorTest"

  val operator = new FileStageOperator(FileStageSettings("/tmp/file-stage-test.txt"))

  it should "properly written to file" in {
    val testSource: Source[Payload, NotUsed] = Source(
      List(
        (NORMAL, createTestEvent("event1")),
        (SKIPPING, createTestEvent("event2")),
        (FAILED(new RuntimeException("failed")), createTestEvent("event3")),
        (STAGING(Some(new RuntimeException("to staging")), "story1:task1"), createTestEvent("event4")),
        (NORMAL, createTestEvent("event5")),
        (STAGING(Some(new IllegalArgumentException("illegal arguments")), "story1:task1"), createTestEvent("event6"))
      )
    )

    val testSource2: Source[Payload, NotUsed] =
      Source(1 to 100000)
        .map(i => (STAGING(Some(new RuntimeException("to staging")), s"story:task$i"), createTestEvent(s"event$i")))

    val result = testSource.via(operator.flow()).runForeach(println)

    Await.result(result, 1 minute)
  }

}
