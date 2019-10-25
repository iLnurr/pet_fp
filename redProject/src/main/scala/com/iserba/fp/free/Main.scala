package com.iserba.fp.free

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App {
  val connectionCheck = Await.result(algebra.checkResult, Duration.Inf)
  println(connectionCheck)
}
