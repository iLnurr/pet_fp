package catsex.ch11

import cats.instances.list._
import cats.instances.map._
import cats.kernel.CommutativeMonoid
import cats.syntax.foldable._
import cats.syntax.semigroup._
import catsex.ch11.algebra.{BoundedSemiLattice, GCounter}  // for combineAll

object oldImpl {
  final case class GCounterSimple(counters: Map[String, Int]) {
    def increment(machine: String, amount: Int): Unit = {
      copy(counters = counters.updated(machine,amount + counters.getOrElse(machine, 0)))
      ()
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


  final case class GCounter2[A](counters: Map[String, A]) {
    def increment(machine: String, amount: A)(implicit cm: CommutativeMonoid[A]): Unit = {
      GCounter2[A](counters = counters.updated(machine,amount |+| counters.getOrElse(machine, cm.empty)))
      ()
    }

    def merge(that: GCounter2[A])(implicit bs: BoundedSemiLattice[A]): GCounter2[A] =
      GCounter2(this.counters |+| that.counters)

    def total(implicit cm: CommutativeMonoid[A]): A =
      counters.values.toList.combineAll
  }

  implicit def gcountermap[K,V] = new GCounter[Map,K,V] {
    def increment(f: Map[K, V])(k: K, v: V)
                 (implicit m: CommutativeMonoid[V]): Map[K, V] =
      f.updated(k, v |+| f.getOrElse(k,m.empty))

    def merge(f1: Map[K, V], f2: Map[K, V])
             (implicit b: BoundedSemiLattice[V]): Map[K, V] =
      f1 |+| f2

    def total(f: Map[K, V])(implicit m: CommutativeMonoid[V]): V =
      f.values.toList.combineAll
  }

  implicit val gcounterMapStringInt: GCounter[Map, String, Int] = gcountermap[String,Int]
}
