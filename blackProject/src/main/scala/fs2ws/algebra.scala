package fs2ws

import cats.tagless.finalAlg
import fs2ws.Domain.Message
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
  def register(c: Client[F]): F[Client[F]]
  def unregister(c: Client[F]): F[Unit]
  def broadcast(message: Message, filterF: Client[F] => Boolean): F[Unit]
  def updateClients(client: Client[F], message: Message): F[(Message, Client[F])]
}

@finalAlg
trait ServerAlgebra[F[_],I,O,StreamPipe] {
  def handler: I => F[O]
  def clients: F[Clients[F]]
  def start(port: Int): F[Unit]
  def pipe: StreamPipe
}
