package catsex.ch8

import cats.Id

object main extends App {
  def testTotalUptime(): Unit = {
    val hosts = Map("host1" -> 10, "host2" -> 6)
    val testClient = new TestUptimeClient(hosts)
    val service: UptimeService[Id] = new UptimeService {
      val client: UptimeClient[Id] = testClient
    }
    val actual = service.getTotalUptime(hosts.keys.toList)
    val expected = hosts.values.sum
    assert(actual == expected)
  }

  testTotalUptime()

}
