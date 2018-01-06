package com.thenetcircle.event_bus.factory

import com.thenetcircle.event_bus.RunningContext
import com.thenetcircle.event_bus.interface._
import com.thenetcircle.event_bus.plots.http.{HttpSinkBuilder, HttpSourceBuilder}
import com.thenetcircle.event_bus.plots.kafka.{KafkaSinkBuilder, KafkaSourceBuilder}

object PlotBuilderFactory {

  def getSourceBuilder(plotType: String): Option[ISourceBuilder] =
    plotType.toUpperCase match {
      case "HTTP"  => Some(new HttpSourceBuilder())
      case "KAFKA" => Some(new KafkaSourceBuilder())
      case _       => None
    }

  def getOpBuilder(plotType: String): Option[IOpBuilder] = plotType.toUpperCase match {
    case "TOPIC_RESOLVER" => None
    case _                => None
  }

  def getSinkBuilder(plotType: String): Option[ISinkBuilder] = plotType.toUpperCase match {
    case "HTTP"  => Some(new HttpSinkBuilder())
    case "KAFKA" => Some(new KafkaSinkBuilder())
    case _       => None
  }

  def buildSource(builderType: String,
                  configString: String)(implicit runningContext: RunningContext): Option[ISource] =
    getSourceBuilder(builderType).map(_builder => _builder.build(configString))

  def buildOp(builderType: String,
              configString: String)(implicit runningContext: RunningContext): Option[IOp] =
    getOpBuilder(builderType).map(_builder => _builder.build(configString))

  def buildSink(builderType: String,
                configString: String)(implicit runningContext: RunningContext): Option[ISink] =
    getSinkBuilder(builderType).map(_builder => _builder.build(configString))

}
