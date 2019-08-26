package ch1

import ch1.algebra.PrintableSyntax._
import ch1.domain._
import ch1.impl._

object main extends App {
  import ch1.algebra.Printable._
  val cat = Cat("c", 1, "red")

  print(cat)
  cat.print

  import cats.Show
  import cats.instances.int._
  import cats.instances.string._
  import cats.syntax.show._
  import impl.catShow

  val showInt: Show[Int] = Show.apply[Int]
  val showString: Show[String] = Show.apply[String]

  showInt.show(123)
  123.show

  println(cat.show)


  import cats.Eq
  import cats.instances.int._
  import cats.instances.option._
  import cats.syntax.eq._

  val eqInt = Eq[Int]
  eqInt.eqv(123,123)
  eqInt.eqv(123,124)

  123 === 123
  123 =!= 124

  Option(1) =!= Option.empty[Int]

  import impl.catEq
  val cat2 = Cat("c2", 2, "black")
  cat =!= cat2
  Option(cat) =!= Option.empty[Cat]
  Option(cat) =!= Option(cat2)

  import cats.syntax.functor._
//  Branch(Leaf(10), Leaf(20)).map(_ * 2)
  Tree.branch(Tree.leaf(10), Tree.leaf(20)).map(_ * 2)

  format(Box("hello world"))

  import ch1.algebra.Codec._
  import ch1.algebra.CodecSyntax._
  encode(123.4)
  123.4.encode
  // res0: String = 123.4
  decode[Double]("123.4")
  // res1: Double = 123.4
  encode(Box(123.4))
  // res2: String = 123.4
  decode[Box[Double]]("123.4")
  // res3: Box[Double] = Box(123.4)

  //check enabled "-Ypartial-unification"
  import cats.instances.function._ // for Functor
  import cats.syntax.functor._     // for map
  val func1: Int => Double = (x: Int)    => x.toDouble
  val func2: Double => Double = (y: Double) => y * 2
  val func3: Int => Double = func1.map(func2)

  import cats.syntax.contravariant._ // for contramap
  // contramap == prepending: (A => X contramap B => A) --> B => X
  //  val func3c: Double => Int = func2.contramap(func1) compiler error
  type <=[B, A] = A => B
  type F[A] = Double <= A
  val func2b: Double <= Double = func2
  val func3c: Double <= Int = func2b.contramap(func1)

  //4.4 error handling
  // Choose error-handling behaviour based on type:
  import cats.syntax.either._

  val result1: LoginResult = User("dave", "passw0rd").asRight
  // result1: LoginResult = Right(User(dave,passw0rd))
  val result2: LoginResult = UserNotFound("dave").asLeft
  // result2: LoginResult = Left(UserNotFound(dave))
  result1.fold(handleError, println)
  // User(dave,passw0rd)
  result2.fold(handleError, println)
  // User not found: dave

  //4.5
  val success = monadError.pure(42)
  // success: ErrorOr[Int] = Right(42)
  val failure = monadError.raiseError("Badness")
  // failure: ErrorOr[Nothing] = Left(Badness)
  monadError.handleError(failure) {
    case "Badness" =>
      monadError.pure("It's ok")
    case _ =>
      monadError.raiseError("It's not ok")
  }
  // res2: ErrorOr[ErrorOr[String]] = Right(Right(It's ok))
  monadError.ensure(success)("Number too low!")(_ > 1000)
  // res3: ErrorOr[Int] = Left(Number too low!)

  import cats.syntax.applicative._ // for pure
  import cats.syntax.applicativeError._ // for raiseError etc
  import cats.syntax.monadError._ // for ensure
  import cats.instances.either._
  val success2 = 42.pure[ErrorOr]
  // success: ErrorOr[Int] = Right(42)
  val failure2 = "Badness".raiseError[ErrorOr, Int]
  // failure: ErrorOr[Int] = Left(Badness)
  success2.ensure("Number to low!")(_ > 1000)
  // res4: Either[String,Int] = Left(Number to low!)
}
