package com.thenetcircle.event_dispatcher.connector

import com.thenetcircle.event_dispatcher.RawEvent

trait SourceAdapter[In] {
  def fit(message: In): RawEvent
}

trait SinkAdapter[Out] {
  def unfit(event: RawEvent): Out
}
