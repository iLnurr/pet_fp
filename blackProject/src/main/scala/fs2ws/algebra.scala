package fs2ws

import java.util.UUID

import cats.Monad
import cats.effect.Concurrent
import cats.effect.concurrent.MVar
import cats.tagless.finalAlg
import fs2ws.Domain.{Message, UserType}

@finalAlg
trait MessageProcessorAlgebra[F[_],I,O] {
  def handler: I => F[O]
}

@finalAlg
trait JsonEncoder[F[_], A] {
  def toJson(value: A): F[String]
}

@finalAlg
trait JsonDecoder[F[_], A] {
  def fromJson(json: String): F[A]
}

case class Client[F[_]: Monad: Concurrent](id: UUID = UUID.randomUUID(),
                                    username: Option[String] = None,
                                    userType: Option[String] = None,
                                    subscribed: Boolean = false) {
  private val msgQueue = MVar[F].empty[Message]
  def add(message: Message): F[Unit] =
    Monad[F].flatMap(msgQueue)(_.put(message))
  def take: F[Message] =
    Monad[F].flatMap(msgQueue)(_.take)
  def privileged: Boolean =
    userType.contains(UserType.ADMIN)
}

@finalAlg
trait Clients[FF[_]] {
  def all: FF[Seq[Client[FF]]]
  def register(c: Client[FF]): FF[Client[FF]]
  def unregister(c: Client[FF]): FF[Unit]
  def update(toUpdate: Client[FF]): FF[Client[FF]]
}

@finalAlg
trait ServerAlgebra[F[_],I,O] {
  def clientsIO: F[Clients[F]]
  def startWS(port: Int): F[Unit]
}
