package com.iserba.fp

import java.util.UUID

import com.iserba.fp.free.compilers._
import com.iserba.fp.model._
import com.iserba.fp.tagless.impl._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object Main extends App {
  def checkResponse(response: Response, request: Request): Unit =
    assert(response == dummyLogic.convert(request))

  def checkFree(request: Request): Unit = {
    val (serverId, channel) = Await.result(runServerFree(), Duration.Inf)
    implicit val connections: Map[UUID, RequestResponseChannel] = Map(
      serverId -> channel
    )

    val checkResult = runClientFree(serverId).flatMap(makeRequest(request, _))

    val response = Await.result(checkResult, Duration.Inf)
    checkResponse(response, request)
  }

  def checkTF(request: Request): Unit = {
    val (serverId, channel @ _) =
      Await.result(serverImpl.runServer(), Duration.Inf)
    val response = Await
      .result(
        clientImpl
          .runClient(serverId)
          .flatMap(_ => clientImpl.makeRequest(request)),
        Duration.Inf
      )
      .get
    checkResponse(response, request)
  }

  val freeResult = checkFree(Request(Some(Event(111, Model(Some(1))))))

  val tfResult = checkTF(Request(Some(Event(111, Model(Some(1))))))
}
