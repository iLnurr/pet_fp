package shapelessex.ch3

import impl._
import shapelessex._

object main extends App {
  def check[A](l: List[A])
              (implicit enc: MyTC[A]): List[A] = {
    val res = l
      .map { a =>
        enc.to(a.tapPrintln()).tapPrintln()
      }.map { b =>
      enc.from(b).tapPrintln()
    }
    assert(l == res, s"$l\n != \n$res")
    res
  }


  println("start")

  check(List("ff", "message"))
  check(List(1, 2, 3, 4))
  check(List(true, false, true))
}
