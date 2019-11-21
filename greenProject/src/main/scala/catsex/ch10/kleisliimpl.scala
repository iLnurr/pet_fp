package catsex.ch10

import cats.data.Kleisli
import cats.instances.either._
import cats.syntax.apply._
import catsex.ch10.algebra.Predicate
import catsex.ch10.impl.Errors // for Monad

object kleisliimpl {
  type Result[A]   = Either[Errors, A]
  type Check[A, B] = Kleisli[Result, A, B]
  // Create a check from a function:
  def check[A, B](func: A => Result[B]): Check[A, B] =
    Kleisli(func)
  // Create a check from a Predicate:
  def checkPred[A](pred: Predicate[Errors, A]): Check[A, A] =
    Kleisli[Result, A, A](pred.run)

  import impl._
  //Now rewrite our username and email valida􏰀on example in terms of Kleisli and Predicate.
  def checkUsername: Check[String, String] =
    checkPred(longerThan(3).and(alphanumeric))

  val splitEmail: Check[String, (String, String)] =
    check(_.split('@') match {
      case Array(name, domain) =>
        Right((name, domain))
      case _ =>
        Left(error("Must contain a single @ character"))
    })

  val checkLeft: Check[String, String] =
    checkPred(longerThan(0))

  val checkRight: Check[String, String] =
    checkPred(longerThan(3).and(contains('.')))

  val joinEmail: Check[(String, String), String] =
    check {
      case (l, r) =>
        (checkLeft(l), checkRight(r)).mapN(_ + "@" + _)
    }
  val checkEmail: Check[String, String] =
    splitEmail.andThen(joinEmail)

  def createUser(username: String, email: String): Either[Errors, User] =
    (checkUsername.run(username), checkEmail.run(email)).mapN(User)

}
