package catsex.ch11

import catsex.ch11.algebra.GCounter // for Monoid
import impl._
import catsex._

object main extends App {

  val g1 = Map("a" -> 7, "b" -> 3)
  val g2 = Map("a" -> 2, "b" -> 5)

  val counter = GCounter[Map, String, Int]
  val merged  = counter.merge(g1, g2)
  merged.println()
  // merged: Map[String,Int] = Map(a -> 7, b -> 5)
  val total = counter.total(merged)(intSumCM)
  total.println()
  // total: Int = 12

}
