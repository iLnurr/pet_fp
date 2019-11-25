package fs2ws

import java.util.UUID

import cats.tagless.finalAlg
import fs2ws.Domain.{DBEntity, Message}
import fs2ws.impl.State._

@finalAlg
trait JsonEncoder[F[_], A] {
  def toJson(value: A): F[String]
}

@finalAlg
trait JsonDecoder[F[_], A] {
  def fromJson(json: String): F[A]
}

@finalAlg
trait Clients[F[_]] {
  def get(id:            UUID): F[Option[Client[F]]]
  def register(c:        Client[F]): F[Client[F]]
  def unregister(c:      Client[F]): F[Unit]
  def broadcast(message: Message, filterF: Client[F] => Boolean): F[Unit]
  def update(toUpdate:   Client[F]): F[Unit]
}

@finalAlg
trait ServerAlgebra[F[_], I, O, StreamPipe[_[_]]] {
  def handler: (I, Client[F]) => F[O]
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
trait MessageReader[F[_]] {
  def consume[A](msgHandler: Message => F[A]): F[List[A]]
}

@finalAlg
trait MessageWriter[F[_]] {
  def send(msg:      Message):      F[Unit]
  def sendMany(msgs: Seq[Message]): F[Unit]
}
