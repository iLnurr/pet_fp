package com.iserba.fp

import com.iserba.fp.free.algebra.{ConnectionAlg, Converter, ServerAlg, runClient}
import com.iserba.fp.free.interpreters.{clientToConnectionInterpreter, connectionToServerInterpreter, serverToFutureInterpreter}
import com.iserba.fp.model.{Event, Model, Request, Response}
import com.iserba.fp.utils.Free.freeMonad
import com.iserba.fp.utils.{Free, Monad}
import com.iserba.fp.utils.StreamProcessHelper.emit

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends App {
  def makeRequest(request: Request, channel: RequestResponseChannel): ParF[model.Response] =
    emit(request)
      .through(channel)
      .runStreamProcess
      .map(_.head)
      .runFree


  def checkFree(request: Request): Response = {
    implicit val connFreeMonad: Monad[({type f[a] = Free[ConnectionAlg, a]})#f] =
      freeMonad[ConnectionAlg]
    implicit val serverAlgMonad: Monad[({type f[a] = Free[ServerAlg, a]})#f] =
      freeMonad[ServerAlg]
    implicit val dummyLogic: Converter[Request, Response] = new Converter[Request, Response] {
      def convert: Request => Response = req => Response(req.entity)
    }

    def run: ParF[RequestResponseChannel] =
      runClient()
        .foldMap(clientToConnectionInterpreter)
        .foldMap(connectionToServerInterpreter)
        .foldMap(serverToFutureInterpreter)

    val checkResult = run.flatMap(makeRequest(request, _))

    Await.result(checkResult, Duration.Inf)
  }

  val freeResult = checkFree(Request(Some(Event(111, Model(Some(1))))))
  println(freeResult)

}
