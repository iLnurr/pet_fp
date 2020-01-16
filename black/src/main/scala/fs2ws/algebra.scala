package fs2ws

import java.util.UUID

import cats.tagless._
import fs2.Stream
import fs2ws.Domain.{DBEntity, Message}

trait Conf[F[_]] {
  def port:                 Int
  def kafkaBootstrapServer: String
  def kafkaGroupId:         String
  def kafkaMessageTopic:    String
  def dbDriver:             String
  def dbUrl:                String
  def dbUser:               String
  def dbPass:               String
}
object Conf {
  def apply[F[_]](implicit inst: Conf[F]): Conf[F] = inst
}

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

trait Clients[F[_]] {
  def get(id:          UUID):        F[Option[WSClient[F]]]
  def register(c:      WSClient[F]): F[WSClient[F]]
  def unregister(c:    WSClient[F]): F[Unit]
  def update(toUpdate: WSClient[F]): F[Unit]
  def subscribed(): F[Seq[WSClient[F]]]
}
object Clients {
  def apply[F[_]](implicit inst: Clients[F]): Clients[F] = inst
}

trait MessageService[F[_]] {
  def process:   Message => F[Either[String, Message]]
  def tableList: F[Message]
}
object MessageService {
  def apply[F[_]](implicit inst: MessageService[F]): MessageService[F] = inst
}

@finalAlg
trait Server[F[_], I, O, StreamPipe[_[_]]] {
  def handler: (I, WSClient[F]) => F[O]
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
  def createTables(): F[Either[Throwable, Int]]
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
