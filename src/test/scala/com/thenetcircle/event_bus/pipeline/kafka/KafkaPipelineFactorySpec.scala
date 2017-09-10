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

package com.thenetcircle.event_bus.pipeline.kafka

import akka.kafka.{ConsumerSettings, ProducerSettings}
import com.thenetcircle.event_bus.pipeline.PipelineConfigFactory
import com.thenetcircle.event_bus.testkit.AkkaBaseSpec
import com.typesafe.config.{Config, ConfigFactory}

class KafkaPipelineFactorySpec extends AkkaBaseSpec {

  behavior of "KafkaPipelineFactory"

  val testPipelineName = "TestKafkaPipeline"
  val testKafkaPipelineFactory = new KafkaPipelineFactory(
    new PipelineConfigFactory(
      Map[String, Config](
        testPipelineName -> ConfigFactory.parseString("""
                                                           |{
                                                           |  type = Kafka
                                                           |  settings {}
                                                           |}
                                                         """.stripMargin)
      ))
  )

  it can "get PipelineSettings from predefined config" in {
    val settings =
      testKafkaPipelineFactory.getPipelineSettings(testPipelineName)
    settings.name shouldEqual testPipelineName
    settings.producerSettings shouldBe a[ProducerSettings[KafkaPipeline.Key,
                                                          KafkaPipeline.Value]]
    settings.consumerSettings shouldBe a[ConsumerSettings[KafkaPipeline.Key,
                                                          KafkaPipeline.Value]]
  }

  it can "get Pipeline from predefined config" in {
    val pipelineSettings =
      testKafkaPipelineFactory.getPipelineSettings(testPipelineName)
    val pipeline =
      testKafkaPipelineFactory.getPipeline(testPipelineName)

    pipeline shouldBe a[KafkaPipeline]
    pipeline.pipelineSettings.name shouldEqual pipelineSettings.name
  }

}
