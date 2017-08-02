package com.thenetcircle.event_dispatcher.connector.scaladsl

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.thenetcircle.event_dispatcher.{ Event, EventFmt }
import com.thenetcircle.event_dispatcher.connector.RedisPubSubSourceSettings
import com.thenetcircle.event_dispatcher.connector.adapter.RedisPubSubSourceAdapter
import com.thenetcircle.event_dispatcher.extractor.Extractor
import com.thenetcircle.event_dispatcher.stage.redis.DefaultRedisSourceSettings
import com.thenetcircle.event_dispatcher.stage.redis.scaladsl.RedisSource

import scala.collection.immutable

object RedisPubSubSource {
  def apply[Fmt <: EventFmt](
      settings: RedisPubSubSourceSettings
  )(implicit extractor: Extractor[Fmt]): Source[Event, NotUsed] = {

    val redisSource = RedisSource(
      DefaultRedisSourceSettings(
        settings.connectionSettings,
        settings.channels.asInstanceOf[immutable.Seq[String]],
        settings.patterns.asInstanceOf[immutable.Seq[String]]
      ),
      settings.bufferSize
    )

    redisSource
      .map(RedisPubSubSourceAdapter.fit)
      .map(extractor.extract)
  }
}