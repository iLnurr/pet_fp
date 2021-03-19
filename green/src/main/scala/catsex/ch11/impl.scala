package catsex.ch11

import cats.kernel.CommutativeMonoid
import catsex.ch11.algebra.{ BoundedSemiLattice, GCounter, KeyValueStore }
import cats.instances.list._
import cats.instances.map._
import cats.syntax.semigroup._
import cats.syntax.foldable._ // for combineAll

object impl {
  implicit val intMaxBS: BoundedSemiLattice[Int] = new BoundedSemiLattice[Int] {
    def combine(a1: Int, a2: Int): Int =
      a1.max(a2)

    def empty: Int =
      0
  }

  implicit val intSumCM: CommutativeMonoid[Int] = new CommutativeMonoid[Int] {
    def combine(a1: Int, a2: Int): Int =
      a1 + a2

    def empty: Int =
      0
  }

  implicit val intProdCM: CommutativeMonoid[Int] = new CommutativeMonoid[Int] {
    def combine(a1: Int, a2: Int): Int =
      a1 * a2

    def empty: Int =
      1
  }

  implicit def setBS[A](): BoundedSemiLattice[Set[A]] =
    new BoundedSemiLattice[Set[A]] {
      def combine(a1: Set[A], a2: Set[A]): Set[A] =
        a1.union(a2)

      def empty: Set[A] =
        Set.empty[A]
    }

  implicit def mapKV: KeyValueStore[Map] = new KeyValueStore[Map] {
    def put[K, V](f: Map[K, V])(k: K, v: V): Map[K, V] =
      f.updated(k, v)

    def get[K, V](f: Map[K, V])(k: K): Option[V] =
      f.get(k)

    def values[K, V](f: Map[K, V]): List[V] =
      f.values.toList
  }

  implicit class KvsOps[F[_, _], K, V](f: F[K, V]) {
    def put(key: K, value: V)(implicit kvs: KeyValueStore[F]): F[K, V] =
      kvs.put(f)(key, value)

    def get(key: K)(implicit kvs: KeyValueStore[F]): Option[V] =
      kvs.get(f)(key)

    def getOrElse(key: K, default: V)(implicit kvs: KeyValueStore[F]): V =
      kvs.getOrElse(f)(key, default)

    def values(implicit kvs: KeyValueStore[F]): List[V] =
      kvs.values(f)
  }

  implicit def gcounterInstance[F[_, _], K, V](
      implicit kvs: KeyValueStore[F],
      km: CommutativeMonoid[F[K, V]]
  ): GCounter[F, K, V] =
    new GCounter[F, K, V] {
      def increment(
          f: F[K, V]
      )(key: K, value: V)(implicit m: CommutativeMonoid[V]): F[K, V] = {
        val total = f.getOrElse(key, m.empty) |+| value
        f.put(key, total)
      }
      def merge(f1: F[K, V], f2: F[K, V])(
          implicit b: BoundedSemiLattice[V]
      ): F[K, V] =
        f1 |+| f2
      def total(f: F[K, V])(implicit m: CommutativeMonoid[V]): V =
        f.values.combineAll
    }

  implicit val gcounterMapStringInt: GCounter[Map, String, Int] =
    gcounterInstance[Map, String, Int]
}
