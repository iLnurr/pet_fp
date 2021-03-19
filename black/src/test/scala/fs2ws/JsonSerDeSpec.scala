package fs2ws

import fs2ws.Domain.{ login, login_successful, remove_table, Message }
import fs2ws.impl.MessageSerDe._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers

class JsonSerDeSpec extends AnyFlatSpec with Matchers {
  behavior.of("JsonEncoder/JsonDecoder")

  it should "properly encode and decode messages" in {
    check(remove_table(0L))
    check(login(username = "un", password = "pwd"))
    check(login_successful("admin"))
  }

  private def check(msg: Message): Assertion = {
    val json    = encodeMsg(msg)
    val decoded = decodeMsg(json)
    decoded.contains(msg) shouldBe true
  }
}
