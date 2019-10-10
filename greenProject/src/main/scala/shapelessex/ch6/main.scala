package shapelessex.ch6

import shapelessex.ch6.algebra.Migration._
import shapelessex.ch6.impl._
import cats.instances.all._

object main extends App {
  case class V1(i: Int, s: String)
  case class V2(i: Int)
  case class V3(i: Int, s: String, d: Double)
  case class V4(s: String, i: Int)

  V1(1,"1").migrateTo[V2].tapPrintln()
  V1(1,"1").migrateTo[V3].tapPrintln()
  V1(1,"1").migrateTo[V4].tapPrintln()
}
