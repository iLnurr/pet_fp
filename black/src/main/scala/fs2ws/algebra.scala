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
trait ClientAlgebra[F[_]] {
  def id:         UUID
  def username:   Option[String]
  def usertype:   Option[String]
  def subscribed: Boolean
  def msgs:       Stream[F, Message]
  def privileged: Boolean
  def updateState(message: Message): ClientAlgebra[F]
}

@finalAlg
trait Clients[F[_]] {
  def get(id:            UUID):             F[Option[ClientAlgebra[F]]]
  def register(c:        ClientAlgebra[F]): F[ClientAlgebra[F]]
  def unregister(c:      ClientAlgebra[F]): F[Unit]
  def broadcast(message: Message):          F[Unit]
  def update(toUpdate:   ClientAlgebra[F]): F[Unit]
}

@finalAlg
trait ServerAlgebra[F[_], I, O, StreamPipe[_[_]]] {
  def handler: (I, ClientAlgebra[F]) => F[O]
  def clients: Clients[F]
  def start(): F[Unit]
  def pipe:    StreamPipe[F]
}

@finalAlg
trait DbReaderAlgebra[F[_], T <: DBEntity] {
  def getById(id:  Long):   F[Option[T]]
  def getByName(n: String): F[Option[T]]
  def list: F[Seq[T]]
}

@finalAlg
trait DbWriterAlgebra[F[_], T <: DBEntity] {
  def add(after_id: Long, ent: T): F[Either[Throwable, T]]
  def update(ent:   T): F[Either[Throwable, Unit]]
  def remove(id:    Long): F[Either[Throwable, Unit]]
}

@finalAlg
trait MessageReaderAlgebra[F[_], A] {
  def consume(): Stream[F, A]
}

@finalAlg
trait MessageWriterAlgebra[F[_], A] {
  type Result
  def send(msg: A): Stream[F, Result]
}
