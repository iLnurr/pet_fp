package fs2ws

import fs2ws.Domain.{login, login_successful, remove_table, Message}
import fs2ws.impl.JsonSerDe.{decoder, encoder}
import org.scalatest.{Assertion, FlatSpec, Matchers}

class JsonSerDeSpec extends FlatSpec with Matchers {
  behavior.of("JsonEncoder/JsonDecoder")

  it should "properly encode and decode messages" in {
    check(remove_table(0L))
    check(login(username = "un", password = "pwd"))
    check(login_successful("admin"))
  }

  private def check(msg: Message): Assertion = {
    val json    = encoder.toJson(msg).unsafeRunSync()
    val decoded = decoder.fromJson(json).unsafeRunSync()
    decoded shouldBe msg
  }
}