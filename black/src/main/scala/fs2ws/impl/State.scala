package fs2ws.impl

import java.util.UUID

import cats.Monad
import cats.effect.concurrent.Ref
import cats.effect.{ConcurrentEffect, ContextShift, Sync, Timer}
import com.typesafe.scalalogging.Logger
import fs2.Stream
import fs2ws.Domain._
import fs2ws.{ClientAlgebra, Clients}

object State {
  private lazy val logger = Logger("clients")

  case class Client[F[_]: Monad: ConcurrentEffect: ContextShift: Timer](
    id:         UUID           = UUID.randomUUID(),
    username:   Option[String] = None,
    usertype:   Option[String] = None,
    subscribed: Boolean        = false
  ) extends ClientAlgebra[F] {
    private val consumer = new MessageReaderImpl[F]

    def msgs: Stream[F, Message] = consumer.consume()

    def privileged: Boolean =
      usertype.contains(UserType.ADMIN)

    def updateState(message: Message): Client[F] =
      message match {
        case _: subscribe_tables =>
          copy(subscribed = true)
        case _: unsubscribe_tables =>
          copy(subscribed = false)
        case login(un, _) =>
          copy(username = Some(un))
        case login_successful(user_type) =>
          copy(usertype = Some(user_type))
        case _: login_failed =>
          Client(id) // clear username, userType, subscribed info
        case _ =>
          this
      }
  }

  import cats.syntax.functor._
  case class ConnectedClients[F[_]: Sync: ConcurrentEffect: ContextShift: Timer](
    val ref: Ref[F, Map[UUID, ClientAlgebra[F]]]
  ) extends Clients[F] {
    private val producer = new MessageWriterImpl[F]
    def register(state: ClientAlgebra[F]): F[ClientAlgebra[F]] = {
      logger.info(s"ConnectedClients: Register $state")
      ref.modify { oldClients =>
        (oldClients + (state.id -> state), state)
      }
    }

    def unregister(c: ClientAlgebra[F]): F[Unit] = {
      logger.info(s"ConnectedClients: Unregister $c")
      ref.update { old =>
        old - c.id
      }
    }

    def broadcast(message: Message): F[Unit] = {
      logger.info(s"ConnectedClients: broadcast $message")
      producer.send(message).compile.drain
    }

    def get(id: UUID): F[Option[ClientAlgebra[F]]] = {
      logger.info(s"ConnectedClients: get $id")
      ref.get.map(_.get(id))
    }

    def update(toUpdate: ClientAlgebra[F]): F[Unit] = {
      logger.info(s"ConnectedClients: update $toUpdate")
      ref
        .modify { old =>
          val updatedClient = old.get(toUpdate.id).map(_ => toUpdate)
          val updatedClients = updatedClient
            .map(cl => old + (toUpdate.id -> cl))
            .getOrElse(old)
          (updatedClients, toUpdate)
        }
        .map(_ => ())
    }
  }

  object ConnectedClients {
    def create[F[_]: Sync: ConcurrentEffect: ContextShift: Timer]
      : F[ConnectedClients[F]] =
      Ref[F]
        .of(Map.empty[UUID, ClientAlgebra[F]])
        .map(ref => new ConnectedClients[F](ref))
  }

}
