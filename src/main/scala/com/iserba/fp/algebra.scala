package com.iserba.fp

import scala.language.{higherKinds, implicitConversions}
import utils.StreamProcess

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
  trait Connection

  trait Server[F[_]] {
    def convert: Req => Resp
    def run(conn: Connection): StreamProcess[F,Resp]
  }

  trait Client[F[_]] {
    def call(request: Req, connection: Connection): StreamProcess[F,Resp]
  }
}