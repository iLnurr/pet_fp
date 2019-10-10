package shapelessex.ch6

import shapelessex.ch6.algebra.Migration._
import shapelessex.ch6.impl._
import cats.instances.all._
import shapeless._

object main extends App {
  case class V1(i: Int, s: String)
  case class V2(i: Int)
  case class V3(i: Int, s: String, d: Double)
  case class V4(s: String, i: Int)

  V1(1,"1").migrateTo[V2].tapPrintln()
  V1(1,"1").migrateTo[V3].tapPrintln()
  V1(1,"1").migrateTo[V4].tapPrintln()

  // records
  import shapeless.record._
  val v1 = LabelledGeneric[V1].to(V1(1,"1"))
  assert(v1.get('i) == 1)
  assert(v1.get('s) == "1")

  val v12 = v1.updated('i,2)
  assert(v12.get('i) == 2)

  val v13 = v12.updateWith('i)(3 + _)
  assert(v13.get('i) == 5)

  val (removedInt, v14) = v1.remove('i)
  assert(removedInt == 1)
  assert(v14.toMap.size == 1)
}
