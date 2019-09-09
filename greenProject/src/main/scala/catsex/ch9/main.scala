package catsex.ch9

import impl._
import catsex._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object main extends App {
  import cats.instances.int._ // for Monoid
  foldMap(Vector(1, 2, 3))(identity).println()
  // res2: Int = 6
  import cats.instances.string._ // for Monoid
  // Mapping to a String uses the concatenation monoid:
  foldMap(Vector(1, 2, 3))(_.toString + "! ").println()
  // res4: String = "1! 2! 3! "

  val future1: Future[Int] =
    parallelFoldMap1((1 to 1000).toVector)(_ * 1000)
  val future2: Future[Int] =
    parallelFoldMap2((1 to 1000).toVector)(_ * 1000)
  Await.result(future1, 1.second).println()
  // res3: Int = 500500000

  Await.result(future2, 1.second).println()
  // res3: Int = 500500000

}
