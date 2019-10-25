package com.iserba.fp

import com.iserba.fp.free.algebra.{ConnectionAlg, ServerAlg, runClient}
import com.iserba.fp.free.interpreters.{clientToConnectionInterpreter, connectionToServerInterpreter, serverToFutureInterpreter}
import com.iserba.fp.model.{Event, Model, Request}
import com.iserba.fp.utils.Free.freeMonad
import com.iserba.fp.utils.{Free, Monad}
import com.iserba.fp.utils.StreamProcessHelper.emit

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App {
  implicit val connFreeMonad: Monad[({type f[a] = Free[ConnectionAlg, a]})#f] =
    freeMonad[ConnectionAlg]
  implicit val serverAlgMonad: Monad[({type f[a] = Free[ServerAlg, a]})#f] =
    freeMonad[ServerAlg]

  def run: ParF[RequestResponseChannel] =
    runClient()
      .foldMap(clientToConnectionInterpreter)
      .foldMap(connectionToServerInterpreter)
      .foldMap(serverToFutureInterpreter)

  val requestResponseChannel = Await.result(run, Duration.Inf)

  def check(request: Request, channel: RequestResponseChannel): ParF[model.Response] =
    emit(request)
      .through(channel)
      .runStreamProcess
      .map(_.head)
      .runFree

  val checkResult = check(Request(Some(Event(111, Model(Some(1))))), requestResponseChannel)

  println(Await.result(checkResult, Duration.Inf))

}
