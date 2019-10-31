package com.iserba.fp

import java.util.UUID

import com.iserba.fp.free.compilers._
import com.iserba.fp.free.impl
import com.iserba.fp.model._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object Main extends App {
  def checkResponse(response: Response, request: Request): Unit =
    assert(response == impl.dummyLogic.convert(request))

  def checkFree(request: Request): Unit = {
    val (serverId, channel) = Await.result(runServerFree(), Duration.Inf)
    implicit val connections: Map[UUID, RequestResponseChannel] = Map(serverId -> channel)

    val checkResult = runClientFree(serverId).flatMap(makeRequest(request, _))

    val response = Await.result(checkResult, Duration.Inf)
    checkResponse(response, request)
  }

  val freeResult = checkFree(Request(Some(Event(111, Model(Some(1))))))

}
