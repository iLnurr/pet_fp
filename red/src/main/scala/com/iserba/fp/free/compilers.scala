package com.iserba.fp.free

import java.util.UUID

import com.iserba.fp._
import com.iserba.fp.free.algebra._
import com.iserba.fp.free.interpreters._
import com.iserba.fp.model._
import com.iserba.fp.utils.Free.freeMonad
import com.iserba.fp.utils.StreamProcessHelper._
import com.iserba.fp.utils.{Free, Monad}

import scala.concurrent.{ExecutionContext, Future}

object compilers {
  implicit val connFreeMonad: Monad[Free[ConnectionAlg, *]] =
    freeMonad[ConnectionAlg]
  implicit val serverFreeMonad: Monad[Free[ServerAlg, *]] =
    freeMonad[ServerAlg]
  private val connectionToFutureInterpreter = new AlgInterpreter[Future]
  private val connectionToServerInterpreter = new AlgInterpreter[ServerAlg]

  // RunClient -> ClientConnect -> (serverChannel)
  def runClientFree(serverId:   UUID)(
    implicit serverConnections: Map[UUID, RequestResponseChannel]
  ): Future[RequestResponseChannel] =
    runClient(serverId)
      .foldMap(clientToConnectionInterpreter)
      .foldMap(connectionToFutureInterpreter.translate(serverConnections))
      .runFree

  // RunServer -> RegisterServer -> ServerChannel -> ServerConnect -> (serverId, serverChannel)
  def runServerFree()(
    implicit ec: ExecutionContext
  ): Future[(UUID, RequestResponseChannel)] =
    runServer()
      .foldMap(serverToConnectionInterpreter)
      .foldMap(connectionToServerInterpreter.translate())
      .foldMap(serverToConnectionInterpreter)
      .foldMap(connectionToFutureInterpreter.translate())
      .runFree
      .map(t => t.id -> t.channel)

  def makeRequest(
    request: Request,
    channel: RequestResponseChannel
  ): Future[model.Response] = // TODO interpreter
    emit(request)
      .through(channel)
      .runStreamProcess
      .map(_.head)
      .runFree
}
