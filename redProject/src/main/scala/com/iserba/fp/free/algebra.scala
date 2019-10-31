package com.iserba.fp.free

import java.util.UUID

import com.iserba.fp.free.algebra._
import com.iserba.fp.free.interpreters.{
  clientToConnectionInterpreter,
  connectionToFutureInterpreter,
  serverToConnectionInterpreter
}
import com.iserba.fp.model._
import com.iserba.fp.utils.Free.{Translate, ~>}
import com.iserba.fp.utils.StreamProcessHelper._
import com.iserba.fp.utils.{Free, Monad}
import com.iserba.fp.{
  IO,
  RequestResponseChannel,
  ResponseStreamByRequest,
  model
}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}

object algebra {
  trait Converter[I,O] {
    def convert: I => O
  }

  sealed trait ServerAlg[T]
  case class RunServer(converter: Converter[Request,Response]) extends ServerAlg[(UUID, RequestResponseChannel)]
  type Server[T] = Free[ServerAlg, T]
  def runServer(converter: Converter[Request,Response]): Server[(UUID, RequestResponseChannel)] =
    Free.liftF(RunServer(converter))
  def createServerChannel(converter: Converter[Request,Response]): RequestResponseChannel =
    constantF[IO, ResponseStreamByRequest](req =>
      emit(converter.convert(req))
    )

  sealed trait ConnectionAlg[T]
  case class ClientConnect(serverId: UUID) extends ConnectionAlg[RequestResponseChannel]
  case class ServerConnect(serverId: UUID, channel: RequestResponseChannel) extends ConnectionAlg[(UUID, RequestResponseChannel)]
  type Connection[T] = Free[ConnectionAlg, T]
  def createClientConnection(serverId: UUID): Connection[RequestResponseChannel] =
    Free.liftF(ClientConnect(serverId))
  def registerServer(serverId: UUID, channel: RequestResponseChannel): Connection[(UUID, RequestResponseChannel)] =
    Free.liftF(ServerConnect(serverId,channel))

  sealed trait ClientAlg[T]
  case class RunClient(serverId: UUID) extends ClientAlg[RequestResponseChannel]
  type Client[T] = Free[ClientAlg, T]
  def runClient(serverId: UUID): Client[RequestResponseChannel] =
    Free.liftF(RunClient(serverId))
}

object interpreters {
  def serverToConnectionInterpreter: Translate[ServerAlg, Connection] = new (ServerAlg ~> Connection) {
    override def apply[A](f: ServerAlg[A]): Connection[A] = f match {
      case RunServer(converter) =>
        registerServer(UUID.randomUUID(), createServerChannel(converter))
    }
  }

  def clientToConnectionInterpreter: Translate[ClientAlg, Connection] = new (ClientAlg ~> Connection) {
    override def apply[A](f: ClientAlg[A]): Connection[A] = f match {
      case RunClient(serverId) =>
        createClientConnection(serverId)
    }
  }

  def connectionToFutureInterpreter(serverConnections: Map[UUID, RequestResponseChannel] = Map())
                                   (implicit ec: ExecutionContext): Translate[ConnectionAlg, Future] =
    new (ConnectionAlg ~> Future) {
      override def apply[A](f: ConnectionAlg[A]): Future[A] = f match {
        case ClientConnect(serverId) =>
          Future(serverConnections(serverId))
        case ServerConnect(serverId, channel) =>
          Future(serverId -> channel)
      }
    }
}

object compilers {
  def runClientFree(serverId: UUID)
                   (implicit
                    monad: Monad[Connection],
                    serverConnections: Map[UUID, RequestResponseChannel],
                    ec: ExecutionContext): Future[RequestResponseChannel] =
    runClient(serverId)
      .foldMap(clientToConnectionInterpreter)
      .foldMap(connectionToFutureInterpreter(serverConnections))

  def runServerFree(converter: Converter[Request, Response])
                   (implicit
                    monad: Monad[Connection],
                    ec: ExecutionContext): Future[(UUID, RequestResponseChannel)] =
    runServer(converter)
      .foldMap(serverToConnectionInterpreter)
      .foldMap(connectionToFutureInterpreter())

  def makeRequest(request: Request, channel: RequestResponseChannel): Future[model.Response] =
    emit(request)
      .through(channel)
      .runStreamProcess
      .map(_.head)
      .runFree
}

