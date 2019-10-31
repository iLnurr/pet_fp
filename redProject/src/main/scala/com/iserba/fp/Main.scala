package com.iserba.fp

import java.util.UUID

import com.iserba.fp.free.algebra._
import com.iserba.fp.free.compilers._
import com.iserba.fp.model.{Event, Model, Request, Response}
import com.iserba.fp.utils.Free.freeMonad
import com.iserba.fp.utils.{Free, Monad}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object Main extends App {
  val dummyLogic: Converter[Request, Response] = new Converter[Request, Response] {
    def convert: Request => Response = req => Response(req.entity.map(ev => ev.copy(ts = ev.ts * 2)))
  }
  def checkResponse(response: Response, request: Request): Unit =
    assert(response == dummyLogic.convert(request))

  def checkFree(request: Request): Unit = {
    implicit val connFreeMonad: Monad[({type f[a] = Free[ConnectionAlg, a]})#f] =
      freeMonad[ConnectionAlg]

    val (serverId, channel) = Await.result(runServerFree(dummyLogic), Duration.Inf)
    implicit val pool: Map[UUID, RequestResponseChannel] = Map(serverId -> channel)

    val checkResult = runClientFree(serverId).flatMap(makeRequest(request, _))

    val response = Await.result(checkResult, Duration.Inf)
    checkResponse(response, request)
  }

  val freeResult = checkFree(Request(Some(Event(111, Model(Some(1))))))

}
