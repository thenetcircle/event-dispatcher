package com.thenetcircle.event_bus.story.interfaces

import akka.stream.scaladsl.Flow
import com.thenetcircle.event_bus.story.{Payload, StoryMat, TaskRunningContext}

trait ISinkableTask {

  def flow()(
      implicit runningContext: TaskRunningContext
  ): Flow[Payload, Payload, StoryMat]

}
