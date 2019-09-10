package catsex.ch10

import catsex.ch10.algebra.{Check, Predicate}
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.syntax.apply._
import cats.syntax.validated._ // for valid and invalid
import cats.instances.string._

object impl {
  type Errors = NonEmptyList[String]
  def error(s: String): NonEmptyList[String] =
    NonEmptyList(s, Nil)
  def longerThan(n: Int): Predicate[Errors, String] =
    Predicate.lift(
      error(s"Must be longer than $n characters"),
      str => str.length > n)
  val alphanumeric: Predicate[Errors, String] =
    Predicate.lift(
      error(s"Must be all alphanumeric characters"),
      str => str.forall(_.isLetterOrDigit))
  def contains(char: Char): Predicate[Errors, String] = Predicate.lift(
    error(s"Must contain the character $char"),
    str => str.contains(char))
  def containsOnce(char: Char): Predicate[Errors, String] = Predicate.lift(
    error(s"Must contain the character $char only once"),
    str => str.count(_ == char) == 1)

  //A username must contain at least four characters and consist en􏰀rely of alphanumeric characters
  def checkUsername: Check[Errors, String,String] =
    Check(longerThan(3) and alphanumeric)

  //An email address must contain an @ sign. Split the string at the @.
  // The string to the left􏰁 must not be empty.
  // The string to the right must be at least three characters long and contain a dot.
  def split(s:String): ValidatedNel[String, (String, String)] = {
    s.split("@") match {
      case Array(l,r) =>
        (l,r).validNel[String]
      case _ =>
        "must contains single @".invalidNel[(String,String)]
    }
  }
  def checkEmail: Check[Errors, String,String] = Check { (email: String) =>
    contains('@').apply(email)
      .andThen{ s =>
        split(s)
      }.andThen{ case (l,r) =>
      longerThan(0)(l)
        .combine(
          longerThan(2)(r) combine contains('.')(r)
        ).andThen(_ => (s"$l@$r").validNel[String])
    }
  }

  final case class User(username: String, email: String)
  def createUser(username: String,
                 email: String): Validated[Errors, User] =
    (checkUsername(username), checkEmail(email)).mapN(User)



}
