package fs2ws

import fs2ws.Domain.{AuthReq, AuthSuccessResp, Msg}
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._


object JsonSerDeTest extends App {
  val authReq = AuthReq(username = "un", password = "pwd")
  val json =
    """
       |{
       |  "$type"    : "login",
       |  "username" : "un",
       |  "password" : "pwd"
       |}
       |""".stripMargin

  println(decode[AuthReq](json).right.get)
  println(authReq.asJson)
  assert(decode[AuthReq](json).right.get == authReq)

  println(parse(json))
  println(parse(json).right.get.as[Msg])

  import fs2ws.impl.JsonSerDe._
  println(incomingMessageDecoder.fromJson(json).unsafeRunSync())
  println(responseEncoder.toJson(AuthSuccessResp("admin")).unsafeRunSync())

}
