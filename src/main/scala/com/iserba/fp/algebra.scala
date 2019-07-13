package com.iserba.fp

import com.iserba.fp.utils.Monad.MonadCatch
import com.iserba.fp.utils.StreamProcess.{Await, Channel, Emit, Halt}

import scala.language.{higherKinds, implicitConversions}
import utils.{Free, StreamProcess}
import com.iserba.fp.utils.StreamProcessHelper._

import scala.annotation.tailrec

object algebra {
  trait Tpe
  trait Metadata {
    def status: Int
  }
  sealed trait Model {
    def id: Option[Long]
  }
  case class Event(tpe: Tpe, ts: Long, metadata: Metadata, model: String)

  trait Request[F[_],A] {
    def entity: F[A]
  }
  trait Response[F[_],A] {
    def body: F[A]
  }

  type Req = Request[Option, Event]
  type Resp = Response[Option, Event]
  trait Connection {
    def request: Option[Req]
    def close: Unit = {
      println(s"Close connection")
    }
  }

  trait Server[F[_]] {
    def conn: IO[Connection]
    def convert: Req => Resp
    def run(): StreamProcess[IO,Resp] =
      runOnce()
    def runOnce(): StreamProcess[IO,Resp] =
      resource_(conn){c =>
        def step = c.request
        def requests: StreamProcess[IO, Resp] = eval(IO(step)).flatMap {
          case None =>
            Halt(End)
          case Some(req) =>
            println(s"Server got req ${req}")
            Emit(convert(req), requests)
        }
        requests
      }{
        c => IO(())
      }

  }

  trait Client[F[_]] {
    def call(request: Req): StreamProcess[F,Resp]
  }
}