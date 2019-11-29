import scala.math._

def bestNum(a: Int, b: Int): Int = {
  def digits(x: Int): List[Int] = {
    x.toString.split("").map(_.toInt).toList
  }
  val aDigits = digits(a)
  val bDigits = digits(b)

  (aDigits, bDigits) match {
    case (List(), List()) =>
      0
    case (List(aa), List(bb)) =>
      max(aa, bb)
    case (listA, listB) if listA.sum == listB.sum =>
      if (a < b) a else b
    case (listA, listB) if listA.sum > listB.sum =>
      a
    case _ =>
      b
  }
}

import scala.annotation.tailrec
def divisors(n: Int) = {
  def factorize(x: BigInt): List[BigInt] = {
    @tailrec
    def foo(x: BigInt, a: BigInt = 2, list: List[BigInt] = Nil): List[BigInt] = a * a > x match {
      case false if x % a == 0 => foo(x / a, a, a :: list)
      case false => foo(x, a + 1, list)
      case true => x :: list
    }

    foo(x)
  }

  def properDivisors(n: BigInt): List[BigInt] = {
    val factors = factorize(n)
    val products = (1 until factors.length).flatMap(i => factors.combinations(i).map(_.product).toList).toList
    (BigInt(1) :: products).filter(_ < n)
  }
  properDivisors(BigInt(n)).map(_.toInt)
}

def bestDivisor(n: Int) = if (n < 10) {
  n
} else{
  val divs = divisors(n)
  val (l1,l2) = divs.sorted.span(_ < 9)
  (l1.max :: l2 ++ List(n)).foldLeft(0)(bestNum)
}

assert(bestNum(12,6) == 6)
assert(bestNum(12,11) == 12)
assert(bestNum(13,31) == 13)

bestDivisor(1)
bestDivisor(100)

divisors(239)

bestDivisor(239)