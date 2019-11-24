package fs2ws

import fs2ws.Domain.{login, login_successful}
import fs2ws.impl.JsonSerDe._

object JsonSerDeTest extends App {
  val authReq = login(username = "un", password = "pwd")
  val json    = encoder.toJson(authReq).unsafeRunSync()
  println(s"encoded $json ")

  println(decoder.fromJson(json).unsafeRunSync())
  println(encoder.toJson(login_successful("admin")).unsafeRunSync())

}
