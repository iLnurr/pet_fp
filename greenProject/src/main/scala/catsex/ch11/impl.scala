package catsex.ch11

import cats.kernel.CommutativeMonoid
import catsex.ch11.algebra.BoundedSemiLattice
import cats.instances.list._   // for Monoid
import cats.instances.map._    // for Monoid
import cats.syntax.semigroup._ // for |+|
import cats.syntax.foldable._  // for combineAll

object impl {
  final case class GCounterSimple(counters: Map[String, Int]) {
    def increment(machine: String, amount: Int): Unit = {
      copy(counters = counters.updated(machine,amount + counters.getOrElse(machine, 0)))
    }
    def merge(that: GCounterSimple): GCounterSimple = {
      def merge(first: Map[String,Int], sec: Map[String,Int]): Map[String,Int] = {
        val mergedByFirst = first.map { case (k, v) =>
          k -> sec.get(k).map(i => math.max(i,v)).getOrElse(v)
        }
        val diffBySec = sec.filterNot{case (k,_) => first.contains(k)}
        mergedByFirst ++ diffBySec
      }
      copy(counters = merge(this.counters, that.counters))
    }
    def total: Int =
      counters.values.sum
  }

  val intBS: BoundedSemiLattice[Int] = new BoundedSemiLattice[Int] {
    def combine(a1: Int, a2: Int): Int =
      a1 max a2

    def empty: Int =
      0
  }


  def setBS[A](): BoundedSemiLattice[Set[A]] = new BoundedSemiLattice[Set[A]] {
    def combine(a1: Set[A], a2: Set[A]): Set[A] =
      a1 union a2

    def empty: Set[A] =
      Set.empty[A]
  }

  final case class GCounter[A](counters: Map[String, A]) {
    def increment(machine: String, amount: A)(implicit cm: CommutativeMonoid[A]): Unit =
      GCounter[A](counters = counters.updated(machine,amount |+| counters.getOrElse(machine, 0)))

    def merge(that: GCounter[A])(implicit bs: BoundedSemiLattice[A]): GCounter[A] =
      GCounter(this.counters |+| that.counters)

    def total(implicit cm: CommutativeMonoid[A]): A =
      counters.values.toList.combineAll
  }
}
