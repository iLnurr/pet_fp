package com.iserba.fp

import scala.language.{higherKinds, implicitConversions}
import java.lang.{Process => _}
import utils.Process

object algebra {
  type Req = String
  type Resp = String
  trait Server[F[_]] {
    def port: Int
    def host: String
    def convert: Req => Resp
    def run: Process[F, Req => Resp]
  }
  trait Client[F[_]] {
    def request(method: String, headers: Seq[(String,String)], queries: Seq[(String,String)], body: String): Process[F,Req]
    def call(port: Int, host: String, request: => Process[F,Req]): Process[F,Resp]
  }
}