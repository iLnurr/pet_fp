package com.iserba.fp.free

import com.iserba.fp.ResponseStream
import com.iserba.fp.free.algebra._
import com.iserba.fp.model._
import com.iserba.fp.utils.Free
import com.iserba.fp.utils.Free.~>
import scala.language.{higherKinds, implicitConversions}

import scala.concurrent.Future

object algebra {
  sealed trait ConnectionAlg[T]
  case class GetRequest(timeout: Long) extends ConnectionAlg[Option[Request]]
  case class AddResponse(resp: Response, req: Request) extends ConnectionAlg[Response]
  case class MakeRequestSync(r: Request) extends ConnectionAlg[Response]
  case class MakeRequestAsync(r: Request) extends ConnectionAlg[Unit]
  case object CloseConnection extends ConnectionAlg[Unit]

  type Connection[T] = Free[ConnectionAlg, T]
  def getRequest(timeout: Long): Connection[Option[Request]] =
    Free.liftF(GetRequest(timeout))
  def addResponse(resp: Response, req: Request): Connection[Response] =
    Free.liftF(AddResponse(resp, req))
  def makeRequest(r: Request): Connection[Response] =
    Free.liftF(MakeRequestSync(r))
  def close(): Connection[Unit] =
    Free.liftF(CloseConnection)

  sealed trait ServerAlg[T]
  case class Convert(r: Request) extends ServerAlg[Response]
  case class RunProcess[S]() extends ServerAlg[S]
  type Server[T] = Free[ServerAlg,T]
  def convert(r: Request): Server[Response] =
    Free.liftF(Convert(r))
  def runProcess[F[_]]()(implicit conn: Connection[Response]): Server[ResponseStream[F]] =
    Free.liftF(RunProcess[ResponseStream[F]]())

  sealed trait ClientAlg[T]
  case class Call[F[_]](r: Request) extends ClientAlg[ResponseStream[F]]
  type Client[T] = Free[ClientAlg,T]
  def call[F[_]](r: Request)(implicit conn: Connection[Response]): Client[ResponseStream[F]] =
    Free.liftF(Call[F](r))
}
object interpreters {
  import scala.concurrent.ExecutionContext.Implicits.global
  case class RequestFired(v: Boolean) {
    def fired = v
  }

//  private var futureInterpreterConnectionCache = Map[Request, (RequestFired, Option[Response])]()
//  val futureInterpreter = new (ConnectionAlg ~> Future) {
//    override def apply[A](f: ConnectionAlg[A]): Future[A] = f match {
//      case GetRequest(timeout) =>
//        println(timeout)
//        Future.successful(Option.empty[Request])
//      case AddResponse(resp, req) =>
//      case MakeRequestSync(r) =>
//      case MakeRequestAsync(r) =>
//      case CloseConnection =>
//    }
//  }
}

