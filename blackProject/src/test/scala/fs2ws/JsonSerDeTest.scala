package fs2ws

import fs2ws.domain.AuthReq
import io.circe.generic.auto._, io.circe.syntax._, io.circe.parser._

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
  assert(decode[AuthReq](json).right.get == authReq)


}
