package fs2ws.impl

import java.util.UUID

import cats.effect.concurrent.Ref
import cats.effect.{ConcurrentEffect, IO, Sync, Timer}
import cats.implicits._
import fs2.Pipe
import fs2.Stream
import fs2ws.Domain._
import fs2ws._
import spinoco.fs2.http.websocket.Frame

class ServerImpl(implicit
                 timer: Timer[IO],
                 ce: ConcurrentEffect[IO],
                 decoder: JsonDecoder[IO, Message],
                 encoder: JsonEncoder[IO, Message],
                 processor: MessageProcessorAlgebra[IO, Message, Message]
                   ) extends ServerAlgebra[IO, Message, Message] {
  private def updateState(message: Message, client: Client[IO]): Client[IO] =
    message match {
      case _: SubscribeTables =>
        client.copy(subscribed = true)
      case _: UnsubscribeTables =>
        client.copy(subscribed = false)
      case AuthReq(un,_,_) =>
        client.copy(username = Some(un))
      case AuthSuccessResp(user_type, _) =>
        client.copy(userType = Some(user_type))
      case _: AuthFailResp =>
        Client(client.id)
      case _ =>
        client
    }
  private def updateClients(clients: Clients[IO], client: Client[IO], message: Message): IO[Message] =
    clients.update(updateState(message, client))
      .map(_ => message)

  override def startWS(port: Int): IO[Unit] =
    FS2Server
      .start[IO](
        pipeF
      )
      .compile
      .drain

  val clientsIO: IO[Clients[IO]] = ConnectedClients.create[IO]

  def pipeF: Pipe[IO,Frame[String], Frame[String]] = input =>
    Stream
      .eval(clientsIO)
      .flatMap{ clients =>
        Stream
          .bracket(clients.register(Client()))(c => clients.unregister(c))
          .flatMap{client =>
            input.evalMap(FS2Server.frameConvert{in =>
              decoder.fromJson(in)
                .flatMap(updateClients(clients, client,_)) // update by incoming
                .flatMap { // process incoming messages
                  case commands: PrivilegedCommands =>
                    if (!client.privileged) IO.pure(NotAuthorized()) else processor.handler(commands)
                  case other =>
                    processor.handler(other)
                }
                .flatMap(msg => IO.pure(msg)) // TODO process output messages
                .flatMap(updateClients(clients, client,_)) // update by response
                .flatMap(encoder.toJson)
            })
          }
      }
}


case class ConnectedClients[F[_]: Sync](ref: Ref[F, Map[UUID, Client[F]]]) extends Clients[F] {
  def get(id: UUID): F[Option[Client[F]]] =
    ref.get.map(_.get(id))
  def all: F[Seq[Client[F]]] =
    ref.get.map(_.values.toList)
  def named: F[List[Client[F]]] =
    ref.get.map(_.values.toList.filter(_.username.isDefined))
  def register(state: Client[F]): F[Client[F]] =
    ref.modify { oldClients =>
      (oldClients + (state.id -> state),state)
    }
  def unregister(id: UUID): F[Option[Client[F]]] =
    ref.modify { old =>
      (old - id, old.get(id))
    }
  def unregister(c: Client[F]): F[Unit] =
    ref.update { old =>
      old - c.id
    }
  def update(toUpdate: Client[F]): F[Client[F]] =
    ref.modify{old =>
      val updatedClient = old.get(toUpdate.id).map(_ => toUpdate)
      val updatedClients = updatedClient
        .map(cl => old + (toUpdate.id -> cl))
        .getOrElse(old)
      (updatedClients, toUpdate)
    }
}
object ConnectedClients {
  def create[F[_]: Sync]: F[ConnectedClients[F]] =
    Ref[F]
      .of(Map.empty[UUID, Client[F]])
      .map(ref => new ConnectedClients[F](ref))
}
