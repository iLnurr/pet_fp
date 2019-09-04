package ch1_7

import cats.{Applicative, Traverse}
import cats.data.{Reader, Validated}

import scala.util.{Failure, Success, Try}

object domain {
  final case class Cat(name: String, age: Int, color: String)

  sealed trait BinaryTree[+A]
  object BinaryTree {
    def branch[A](left: BinaryTree[A], right: BinaryTree[A]): BinaryTree[A] =
      BinaryBranch(left,right)
    def leaf[A](a: A): BinaryTree[A] =
      BinaryLeaf(a)
  }
  final case class BinaryBranch[A](left: BinaryTree[A], right: BinaryTree[A]) extends BinaryTree[A]
  final case class BinaryLeaf[A](value: A) extends BinaryTree[A]

  import cats.instances.vector._
  sealed trait Tree[+T] {
    def headOption: Option[T]
    def isEmpty: Boolean
    def traverse[F[_]: Applicative, B](f: T => F[B]): F[Tree[B]] = this match {
      case NonEmptyTree(head, children) =>
        val h: F[B] = f(head)
        val ch: F[Vector[Tree[B]]] = Traverse[Vector].traverse(children)(_.traverse(f))
        Applicative[F].map2(h, ch)(Tree(_,_))
      case EmptyTree =>
        Applicative[F].pure(Tree())
    }
  }

  object Tree {
    def apply[T](): Tree[T] = EmptyTree
    def apply[T](head: T, children: Vector[Tree[T]]): Tree[T] = NonEmptyTree(head, children)
    def apply[T](head: T, children: Tree[T]*): Tree[T] = NonEmptyTree(head, children.toVector)

    def unapply[T](tree: Tree[T]): Option[(T, Vector[Tree[T]])] = tree match {
      case NonEmptyTree(head, children) => Some((head, children))
      case EmptyTree => None
    }
  }

  case class NonEmptyTree[+T](head: T, children: Vector[Tree[T]]) extends Tree[T] {
    def headOption: Some[T] = Some(head)
    def isEmpty = false
  }

  object NonEmptyTree {
    def apply[T](head: T, children: Tree[T]*): NonEmptyTree[T] = NonEmptyTree(head, children.toVector)
  }

  case object EmptyTree extends Tree[Nothing] {
    def headOption = None
    def isEmpty = true
  }

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
