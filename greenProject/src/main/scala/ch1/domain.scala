package ch1

import cats.data.{Reader, Validated}

import scala.util.{Failure, Success, Try}

object domain {
  final case class Cat(name: String, age: Int, color: String)

  sealed trait Tree[+A]
  object Tree {
    def branch[A](left: Tree[A], right: Tree[A]): Tree[A] =
      Branch(left,right)
    def leaf[A](a: A): Tree[A] =
      Leaf(a)
  }
  final case class Branch[A](left: Tree[A], right: Tree[A]) extends Tree[A]
  final case class Leaf[A](value: A) extends Tree[A]

  final case class Box[A](value: A)

  //4.4 error handling
  type Result[A] = Either[Throwable, A] // 1 way

  sealed trait LoginError extends Product with Serializable // 2 way
  final case class UserNotFound(username: String) extends LoginError
  final case class PasswordIncorrect(username: String) extends LoginError
  case object UnexpectedError extends LoginError
  case class UserLogin(username: String, password: String)
  type LoginResult = Either[LoginError, UserLogin]
  def handleError(error: LoginError): Unit =
    error match {
      case UserNotFound(u) =>
        println(s"User not found: $u")
      case PasswordIncorrect(u) =>
        println(s"Password incorrect: $u")
      case UnexpectedError =>
        println(s"Unexpected error")
    }

  //4.5
  import cats.MonadError
  import cats.instances.either._ // for MonadError
  type ErrorOr[A] = Either[String, A]
  val monadError: MonadError[ErrorOr, String] = MonadError[ErrorOr, String]

  //4.8
  import cats.syntax.applicative._
  case class Db(
                 usernames: Map[Int, String],
                 passwords: Map[String, String]
               )
  type DbReader[T] = Reader[Db, T]
  object DbReader {
    def apply[T](function: Db => T): DbReader[T] =
      Reader[Db,T](db => function(db))
    def empty[T](t: T): DbReader[T] =
      t.pure[DbReader]
  }
  def findUsername(userId: Int): DbReader[Option[String]] =
    DbReader(_.usernames.get(userId))
  def checkPassword(
                     username: String,
                     password: String): DbReader[Boolean] =
    DbReader(_.passwords.get(username).contains(password))
  def checkLogin(
                  userId: Int,
                  password: String): DbReader[Boolean] =
    findUsername(userId).flatMap {
      case Some(username) =>
        checkPassword(username, password)
      case None =>
        DbReader.empty(false)
    }

  // 6.4.4
  import cats.syntax.validated._
  import cats.syntax.either._

  case class User(name: String, age: Int)
  type FormData = Map[String, String]
  type FailFast[A] = Either[List[String], A]
  type FailSlow[A] = Validated[List[String], A]

  def readName(m: FormData): FailFast[String] =
    (m.get("name") match {
      case Some(value) if value.isEmpty=>
        List("Name is blank").invalid[String]
      case Some(name) =>
        name.valid
      case None =>
        List("Name not found").invalid[String]
    }).toEither
  def readAge(m: FormData): FailFast[Int] =
    (m.get("age") match {
      case Some(value) =>
        Try(value.toInt) match {
          case Failure(exception) =>
            List(exception.getMessage).invalid[Int]
          case Success(value) if value < 0 =>
            List(s"Negative age $value").invalid[Int]
          case Success(value) =>
            value.valid[List[String]]
        }
      case None =>
        List("Age not found").invalid[Int]
    }).toEither

  import cats.instances.list._ // for Semigroupal
  import cats.syntax.apply._   // for mapN
  def readUser(m: FormData): FailSlow[User] =
    (
      readName(m).toValidated,
      readAge(m).toValidated
      ).mapN(
      User.apply
    )
}
