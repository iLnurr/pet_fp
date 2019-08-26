package ch1

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
  case class User(username: String, password: String)
  type LoginResult = Either[LoginError, User]
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
}
