package com.iserba.fp

import com.iserba.fp.utils.StreamProcess.{Channel, Emit, Halt}

import scala.language.{higherKinds, implicitConversions}
import utils.StreamProcess
import com.iserba.fp.utils.StreamProcessHelper._

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
    def requests: List[Req]
    def close: Unit = {
      println(s"Close connection")
    }
  }

  trait Server[F[_]] {
    def conn: F[Connection]
    def convert: Req => Resp
    def run(): StreamProcess[F,Resp] =
      eval(conn).flatMap {c =>
        val requests: StreamProcess[F, Resp] = c.requests match {
          case Nil =>
            Halt[F,Resp](End)
          case reqs =>
            val req = reqs.head
            println(s"Server got req ${req}")
            Emit[F,Resp](convert(req), Halt(End))
        }
        requests
      }

  }

  trait Client[F[_]] {
    def call(request: Req): StreamProcess[F,Resp]
  }
}