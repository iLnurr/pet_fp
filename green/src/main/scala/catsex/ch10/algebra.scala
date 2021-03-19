package catsex.ch10

import cats.Semigroup
import cats.data.Validated
import cats.data.Validated._
import cats.syntax.apply._
import cats.syntax.validated._
import cats.syntax.semigroup._
import catsex.ch10.algebra.Check.{ AndThenCheck, FlatMapCheck, MapCheck }
import catsex.ch10.algebra.Predicate.{ And, Or, Pure } // for valid and invalid

object algebra {
  sealed trait Predicate[E, A] {
    def and(that: Predicate[E, A]): Predicate[E, A] =
      And(this, that)
    def or(that: Predicate[E, A]): Predicate[E, A] =
      Or(this, that)
    def apply(a: A)(implicit s: Semigroup[E]): Validated[E, A] = this match {
      case Pure(func) =>
        func(a)
      case And(left, right) =>
        (left(a), right(a)).mapN((_, _) => a)
      case Or(left, right) =>
        left(a) match {
          case Valid(a1 @ _) => Valid(a)
          case Invalid(e1) =>
            right(a) match {
              case Valid(a2 @ _) => Valid(a)
              case Invalid(e2)   => Invalid(e1 |+| e2)
            }
        }
    }

    def run(implicit s: Semigroup[E]): A => Either[E, A] =
      apply(_).toEither
  }
  object Predicate {
    final case class And[E, A](left: Predicate[E, A], right: Predicate[E, A]) extends Predicate[E, A]

    final case class Or[E, A](left: Predicate[E, A], right: Predicate[E, A]) extends Predicate[E, A]

    final case class Pure[E, A](func: A => Validated[E, A]) extends Predicate[E, A]

    def apply[E, A](f: A => Validated[E, A]): Predicate[E, A] =
      Pure(f)
    def lift[E, A](err: E, fn: A => Boolean): Predicate[E, A] =
      Pure(a => if (fn(a)) a.valid else err.invalid)
  }

  sealed trait Check[E, A, B] {
    def apply(a: A)(implicit s: Semigroup[E]): Validated[E, B]

    def map[C](func: B => C): Check[E, A, C] =
      MapCheck(this, func)
    def flatMap[C](func: B => Check[E, A, C]): Check[E, A, C] =
      FlatMapCheck(this, func)
    def andThen[C](that: Check[E, B, C]): Check[E, A, C] =
      AndThenCheck(this, that)

  }
  object Check {
    final case class MapCheck[E, A, B, C](c: Check[E, A, B], f: B => C) extends Check[E, A, C] {
      def apply(a: A)(implicit s: Semigroup[E]): Validated[E, C] =
        c(a).map(f)
    }
    final case class FlatMapCheck[E, A, B, C](
        c: Check[E, A, B],
        f: B => Check[E, A, C]
    ) extends Check[E, A, C] {
      def apply(a: A)(implicit s: Semigroup[E]): Validated[E, C] =
        c(a).withEither(_.flatMap(f(_)(a).toEither))
    }
    final case class PureValidate[E, A, B](func: A => Validated[E, B]) extends Check[E, A, B] {
      def apply(a: A)(implicit s: Semigroup[E]): Validated[E, B] =
        func(a)
    }
    final case class PurePredicate[E, A](pred: Predicate[E, A]) extends Check[E, A, A] {
      def apply(a: A)(implicit s: Semigroup[E]): Validated[E, A] =
        pred(a)
    }
    def apply[E, A](pred: Predicate[E, A]): Check[E, A, A] =
      PurePredicate(pred)
    def apply[E, A, B](func: A => Validated[E, B]): Check[E, A, B] =
      PureValidate(func)
    final case class AndThenCheck[E, A, B, C](
        c1: Check[E, A, B],
        c2: Check[E, B, C]
    ) extends Check[E, A, C] {
      def apply(a: A)(implicit s: Semigroup[E]): Validated[E, C] =
        c1(a).withEither(_.flatMap(b => c2(b).toEither))
    }
  }
}
