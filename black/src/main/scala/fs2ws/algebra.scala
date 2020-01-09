package fs2ws

import java.util.UUID

import cats.tagless.finalAlg
import fs2ws.Domain.{DBEntity, Message}
import fs2.Stream

@finalAlg
trait JsonEncoder[F[_], A] {
  def toJson(value: A): F[String]
}

@finalAlg
trait JsonDecoder[F[_], A] {
  def fromJson(json: String): F[A]
}

@finalAlg
trait WSClient[F[_]] {
  def id: UUID
  def add(message: Message): Unit
  def take():     Seq[Message]
  def subscribed: Boolean
  def privileged: Boolean
  def updateState(message: Message): WSClient[F]
}

@finalAlg
trait Clients[F[_]] {
  def get(id:          UUID):        F[Option[WSClient[F]]]
  def register(c:      WSClient[F]): F[WSClient[F]]
  def unregister(c:    WSClient[F]): F[Unit]
  def update(toUpdate: WSClient[F]): F[Unit]
  def subscribed(): F[Seq[WSClient[F]]]
}

@finalAlg
trait Server[F[_], I, O, StreamPipe[_[_]]] {
  def handler: (I, WSClient[F]) => F[O]
  def clients: Clients[F]
  def start(): F[Unit]
  def pipe:    StreamPipe[F]
}

@finalAlg
trait DbReader[F[_], T <: DBEntity] {
  def getById(id:  Long):   F[Option[T]]
  def getByName(n: String): F[Option[T]]
  def list: F[Seq[T]]
}

@finalAlg
trait DbWriter[F[_], T <: DBEntity] {
  def add(after_id: Long, ent: T): F[Either[Throwable, T]]
  def update(ent:   T): F[Either[Throwable, Unit]]
  def remove(id:    Long): F[Either[Throwable, Unit]]
}

@finalAlg
trait MessageReader[F[_], A] {
  def consume(): Stream[F, A]
}

@finalAlg
trait MessageWriter[F[_], A] {
  type Result
  def send(msg: A): Stream[F, Result]
}
