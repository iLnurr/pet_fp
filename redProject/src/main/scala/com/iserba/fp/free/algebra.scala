package com.iserba.fp.free

import com.iserba.fp.free.algebra._
import com.iserba.fp.model._
import com.iserba.fp.utils.Free.{Translate, ~>}
import com.iserba.fp.utils.StreamProcessHelper._
import com.iserba.fp.utils.{Free, StreamProcess}
import com.iserba.fp.{IO, RequestResponseChannel, ResponseStream}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}

object algebra {
  trait Converter[I,O] {
    def convert: I => O
  }

  sealed trait ServerAlg[T]
  case class ConnectClient() extends ServerAlg[RequestResponseChannel]
  type Server[T] = Free[ServerAlg, T]
  def connect(): Server[RequestResponseChannel] =
    Free.liftF(ConnectClient())

  sealed trait ConnectionAlg[T]
  case class ClientChannel() extends ConnectionAlg[RequestResponseChannel]
  type Connection[T] = Free[ConnectionAlg, T]
  def createConnection(): Connection[RequestResponseChannel] =
    Free.liftF(ClientChannel())

  sealed trait ClientAlg[T]
  case class RunClient() extends ClientAlg[RequestResponseChannel]
  type Client[T] = Free[ClientAlg, T]
  def runClient(): Client[RequestResponseChannel] =
    Free.liftF(RunClient())
}

object interpreters {
  def clientToConnectionInterpreter: Translate[ClientAlg, Connection] = new (ClientAlg ~> Connection) {
    override def apply[A](f: ClientAlg[A]): Connection[A] = f match {
      case RunClient() =>
        createConnection()
    }
  }

  def connectionToServerInterpreter: Translate[ConnectionAlg, Server] = new (ConnectionAlg ~> Server) {
    override def apply[A](f: ConnectionAlg[A]): Server[A] = f match {
      case ClientChannel() =>
        connect()
    }
  }


  def serverToFutureInterpreter(implicit ec: ExecutionContext, converter: Converter[Request,Response]): Translate[ServerAlg,Future]  = new (ServerAlg ~> Future) {
    override def apply[A](f: ServerAlg[A]): Future[A] = f match {
      case ConnectClient() =>
        type Processor = Request => ResponseStream
        def constantProcessor: StreamProcess[IO, Processor] = constantF[IO, Processor](req => emit(converter.convert(req))) // here logic to convert Event to Event
        def connectionChannel(): RequestResponseChannel =
          constantProcessor
        Future(connectionChannel())
    }
  }
}

