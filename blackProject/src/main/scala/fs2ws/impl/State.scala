package fs2ws.impl

import java.util.UUID

import cats.Monad
import cats.effect.concurrent.Ref
import cats.effect.{Concurrent, Sync}
import fs2ws.Clients
import fs2ws.Domain._

import scala.collection.mutable.ListBuffer

object State {

  case class Client[F[_] : Monad : Concurrent](id: UUID = UUID.randomUUID(),
                                               username: Option[String] = None,
                                               userType: Option[String] = None,
                                               subscribed: Boolean = false) {
    private val msgQueue = ListBuffer[Message]()
    def add(message: Message): Unit = {
      println(s"Client $id: Add $message")
      msgQueue.append(message)
    }

    def take: List[Message] = {
      val result = msgQueue.toList
      msgQueue.clear()
      result
    }

    def privileged: Boolean =
      userType.contains(UserType.ADMIN)

    def updateState(message: Message): Client[F] =
      message match {
        case _: SubscribeTables =>
          copy(subscribed = true)
        case _: UnsubscribeTables =>
          copy(subscribed = false)
        case AuthReq(un, _, _) =>
          copy(username = Some(un))
        case AuthSuccessResp(user_type, _) =>
          copy(userType = Some(user_type))
        case _: AuthFailResp =>
          Client(id) // clear username, userType, subscribed info
        case _ =>
          this
      }
  }

  import cats.syntax.functor._
  case class ConnectedClients[F[_] : Sync](val ref: Ref[F, Map[UUID, Client[F]]]) extends Clients[F] {
    def register(state: Client[F]): F[Client[F]] = {
      println(s"ConnectedClients: Register $state")
      ref.modify { oldClients =>
        (oldClients + (state.id -> state), state)
      }
    }

    def unregister(c: Client[F]): F[Unit] = {
      println(s"ConnectedClients: Unregister $c")
      ref.update { old =>
        old - c.id
      }
    }

    def broadcast(message: Message, filterF: Client[F] => Boolean): F[Unit] = {
      ref
        .get
        .map { all =>
          val filtered = all.values.toList.filter(filterF)
          println(s"Broadcast $message to clients ${filtered}")
          filtered.foreach(_.add(message))
        }
    }

    def get(id: UUID): F[Option[Client[F]]] =
      ref.get.map(_.get(id))

    def update(toUpdate: Client[F]): F[Unit] =
      ref.modify { old =>
        val updatedClient = old.get(toUpdate.id).map(_ => toUpdate)
        val updatedClients = updatedClient
          .map(cl => old + (toUpdate.id -> cl))
          .getOrElse(old)
        (updatedClients, toUpdate)
      }.map(_ => ())
  }

  object ConnectedClients {
    def create[F[_] : Sync]: F[ConnectedClients[F]] =
      Ref[F]
        .of(Map.empty[UUID, Client[F]])
        .map(ref => new ConnectedClients[F](ref))
  }

}
