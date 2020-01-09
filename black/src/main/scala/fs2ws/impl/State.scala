package fs2ws.impl

import java.util.UUID
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import cats.Monad
import cats.effect.concurrent.Ref
import cats.effect.{ConcurrentEffect, ContextShift, Sync, Timer}
import com.typesafe.scalalogging.Logger
import fs2ws.Domain._
import fs2ws.{Clients, WSClient}

import scala.collection.mutable.ListBuffer

object State {
  private lazy val logger = Logger("clients")

  case class Client[F[_]: Monad: ConcurrentEffect: ContextShift: Timer](
    id: UUID = UUID.randomUUID()
  ) extends WSClient[F] {
    private val subscribedRef = new AtomicBoolean(false)
    private val usernameRef   = new AtomicReference[Option[String]](None)
    private val usertypeRef   = new AtomicReference[Option[String]](None)
    private val msgQueue      = ListBuffer[Message]()

    def add(message: Message): Unit = {
      logger.info(s"Add $message")
      msgQueue.append(message)
    }
    def take(): Seq[Message] = {
      val result = msgQueue.toList
      msgQueue.clear()
      if (result.nonEmpty) logger.info(s"Take ${result.mkString("\n")}")
      result
    }

    def subscribed: Boolean =
      subscribedRef.get()
    def privileged: Boolean =
      usertypeRef.get().contains(UserType.ADMIN)
    def updateState(message: Message): WSClient[F] =
      message match {
        case _: subscribe_tables =>
          subscribedRef.set(true)
          this
        case _: unsubscribe_tables =>
          subscribedRef.set(false)
          this
        case login(un, _) =>
          usernameRef.set(Some(un))
          this
        case login_successful(user_type) =>
          usertypeRef.set(Some(user_type))
          this
        case _: login_failed =>
          subscribedRef.set(false)
          usertypeRef.set(None)
          usernameRef.set(None)
          this // clear username, userType, subscribed info
        case _ =>
          this
      }

    override def toString: String =
      s"Client(id=${id},username=${usernameRef.get()},usertype=${usertypeRef
        .get()},subscribed=${subscribedRef.get()})"
  }

  import cats.syntax.functor._
  case class ConnectedClients[F[_]: Sync: ConcurrentEffect: ContextShift: Timer](
    ref: Ref[F, Map[UUID, WSClient[F]]]
  ) extends Clients[F] {
    def register(state: WSClient[F]): F[WSClient[F]] = {
      logger.info(s"ConnectedClients: Register $state")
      ref.modify { oldClients =>
        (oldClients + (state.id -> state), state)
      }
    }

    def subscribed(): F[Seq[WSClient[F]]] =
      ref.get.map(_.values.filter(_.subscribed).toSeq)

    def unregister(c: WSClient[F]): F[Unit] = {
      logger.info(s"ConnectedClients: Unregister $c")
      ref.update { old =>
        old - c.id
      }
    }

    def get(id: UUID): F[Option[WSClient[F]]] = {
      logger.info(s"ConnectedClients: get $id")
      ref.get.map(_.get(id))
    }

    def update(toUpdate: WSClient[F]): F[Unit] = {
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
        .of(Map.empty[UUID, WSClient[F]])
        .map(ref => new ConnectedClients[F](ref))
  }

}
