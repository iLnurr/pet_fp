package shapelessex.ch6

import algebra.Migration._
import impl._

object main extends App {
  case class V1(i: Int, s: String)
  case class V2(i: Int)
  case class V3(i: Int, s: String, d: Double)

  V1(1,"1").migrateTo[V2].tapPrintln()
//  V1(1,"1").migrateTo[V3].tapPrintln()
}
