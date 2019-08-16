package fs2ws

import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup
import java.util.UUID
import java.util.concurrent.Executors

import cats.effect.concurrent.Ref
import cats.implicits._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._
import cats.effect.{Concurrent, ConcurrentEffect, IO, Sync, Timer}
import fs2._
import fs2ws._
import scodec.Attempt.{Failure, Successful}
import scodec.bits.ByteVector
import scodec.{Codec, Err}
import spinoco.fs2.http
import spinoco.fs2.http._
import spinoco.fs2.http.websocket.Frame
import spinoco.fs2.http.websocket.Frame.Text
import spinoco.protocol.http.Uri.Path
import spinoco.protocol.http._

import scala.concurrent.duration._

object FS2Server {
  case class ConnectedClient[F[_]](stream: Stream[F,HttpResponse[F]],
                                   id: UUID = UUID.randomUUID(),
                                   username: Option[String] = None,
                                   userType: Option[String] = None)
  case class Clients[F[_]: Sync](ref: Ref[F, Map[UUID, ConnectedClient[F]]]) {
    def get(id: UUID): F[Option[ConnectedClient[F]]] = ref.get.map(_.get(id))
    def all: F[List[ConnectedClient[F]]] = ref.get.map(_.values.toList)
    def named: F[List[ConnectedClient[F]]] =
      ref.get.map(_.values.toList.filter(_.username.isDefined))
    def register(state: ConnectedClient[F]): F[Unit] =
      ref.update { oldClients =>
        oldClients + (state.id -> state)
      }
    def unregister(id: UUID): F[Option[ConnectedClient[F]]] =
      ref.modify { old =>
        (old - id, old.get(id))
      }
    def setUsername(clientId: UUID, username: String) =
      ref.modify { clientsById =>
        val updatedClient =
          clientsById.get(clientId).map(_.copy(username = Some(username)))
        val updatedClients = updatedClient
          .map(c => clientsById + (clientId -> c))
          .getOrElse(clientsById)
        (updatedClients, username)
      }

//    def broadcast(cmd: Protocol.ServerCommand): F[Unit] =
//      named.flatMap(_.traverse_(_.messageSocket.write1(cmd)))
  }
  object Clients {
    def create[F[_]: Sync]: F[Clients[F]] =
      Ref[F]
        .of(Map.empty[UUID, ConnectedClient[F]])
        .map(ref => new Clients(ref))
  }

  implicit val AG: AsynchronousChannelGroup =
    AsynchronousChannelGroup
      .withThreadPool(Executors.newCachedThreadPool(util.mkThreadFactory("fs2-http-spec-AG", daemon = true)))

  implicit val codec: Codec[String] = scodec.codecs.bytes.exmap[String](
    bv => bv.decodeUtf8 match {
      case Left(exception) => Failure(Err(exception.getMessage))
      case Right(str) => Successful(str)
    }
    , b => ByteVector.encodeUtf8(b) match {
      case Left(exception) => Failure(Err(exception.getMessage))
      case Right(vector) => Successful(vector)
    }
  )

  def service[F[_]](request: HttpRequestHeader, body: Stream[F,Byte])
                   (wsPipe: PipeWithState[F, Frame[String], Frame[String]])
                   (clients: Clients[F])
                   (implicit conc: Concurrent[F], timer: Timer[F]): Stream[F, HttpResponse[F]] = {
    request.path match {
      case Path(true, false, Seq("ws_api")) =>
        val stream = websocket.server[F,String,String](input => wsPipe(input, clients)._1, 1.second)(request, body)
        val client = ConnectedClient(stream)
        clients.register(client)
        stream.onFinalize(conc.delay{
          println(s"WS DONE for client=$client")
          clients.unregister(client.id)
        })
      case other =>
        println(s"Client try to connect to path=${other.segments.mkString("/","/","")}. \nBad request=$request")
        Stream.empty
    }
  }

  type PipeWithState[F[_], -I, +O] = (Stream[F, I],Clients[F]) => (Stream[F, O],Clients[F])
  def start[F[_]: ConcurrentEffect: Timer](wsPipe: PipeWithState[F, Frame[String], Frame[String]])= {
    Stream.eval(Clients.create[F])
      .flatMap(clients =>
        http.server[F]
        (new InetSocketAddress("127.0.0.1", 9000))
        (service(_, _)
        (wsPipe)
        (clients)))
  }
  def frameConvert(func: String => String): Frame[String] => Frame[String] = { f =>
    println(s"Server got request: ${f.a}")
    val resp = func(f.a)
    println(s"Server response: $resp")
    Text(resp)
  }

  def dummyWsPipe[F[_]](convertF: String => String): PipeWithState[F, Frame[String], Frame[String]] = (input, clients) =>
    (input.map(frameConvert(convertF)),clients)
}