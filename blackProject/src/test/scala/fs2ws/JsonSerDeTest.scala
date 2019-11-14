package fs2ws

import fs2ws.Domain.{AuthReq, AuthSuccessResp}
import fs2ws.impl.JsonSerDe._


object JsonSerDeTest extends App {
  val authReq = AuthReq(username = "un", password = "pwd")
  val json = encoder.toJson(authReq).unsafeRunSync()
  println(s"encoded $json ")

  println(incomingMessageDecoder.fromJson(json).unsafeRunSync())
  println(encoder.toJson(AuthSuccessResp("admin")).unsafeRunSync())

}
