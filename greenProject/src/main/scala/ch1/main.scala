package ch1

import cats.{Eval, Id}
import cats.data.{Writer, WriterT}
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
  import cats.instances.function._
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

  import cats.instances.either._
  import cats.syntax.applicative._
  import cats.syntax.applicativeError._
  val success2 = 42.pure[ErrorOr]
  // success: ErrorOr[Int] = Right(42)
  val failure2 = "Badness".raiseError[ErrorOr, Int]
  // failure: ErrorOr[Int] = Left(Badness)
  success2.ensure("Number to low!")(_ > 1000)
  // res4: Either[String,Int] = Left(Number to low!)

  //Eval
  /** The naive implementati􏰀on of foldRight below is not stack safe. Make it so using Eval: */
  def foldRightNotStackSafe[A, B](as: List[A], acc: B)(fn: (A, B) => B): B = as match {
    case head :: tail =>
      fn(head, foldRightNotStackSafe(tail, acc)(fn))
    case Nil =>
      acc
  }
  def foldRightEval[A, B](as: List[A], acc: B)(fn: (A, B) => B): Eval[B] = as match {
    case Nil =>
      Eval.now(acc)
    case head :: tail =>
      Eval
        .defer(foldRightEval(tail, acc)(fn))
        .map(fn(head,_))
  }
  def foldRight[A, B](as: List[A], acc: B)(fn: (A, B) => B): B =
    foldRightEval(as,acc)(fn).value

  println(foldRight((1 to 100000).toList, 0L)(_ + _))

  //Writer
  import cats.instances.vector._   // for Monoid
  import cats.syntax.applicative._ // for pure
  type Writer[W, A] = WriterT[Id, W, A]
  type Logged[A] = Writer[Vector[String], A]

  import cats.syntax.writer._ // for tell
  val res = Vector("msg1", "msg2", "msg3").tell
  val rr = res.run
  println(rr)

  import cats.syntax.writer._ // for writer
  val a = Writer(Vector("msg1", "msg2", "msg3"), 123)
  val b = 123.writer(Vector("msg1", "msg2", "msg3"))

  val aResult: Int =
    a.value
  // aResult: Int = 123
  val aLog: Vector[String] =
    a.written
  // aLog: Vector[String] = Vector(msg1, msg2, msg3)
  val (log, result) = b.run
  // log: scala.collection.immutable.Vector[String] = Vector(msg1, msg2, msg3)
  // result: Int = 123

  def slowly[A](body: => A) =
    try body finally Thread.sleep(100)
  def factorial(n: Int): Int = {
    val ans = slowly(if(n == 0) 1 else n * factorial(n - 1))
    println(s"fact $n $ans")
    ans
  }

  /** Rewrite factorial so it captures the log messages in a Writer.
    * Demonstrate that this allows us to reliably separate the logs for concurrent computations.
    * */
  import scala.concurrent._
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
  Await.result(Future.sequence(Vector(
    Future(factorial(3)),
    Future(factorial(3))
  )), 5.seconds)

  def factorialWriter(n: Int): Logged[Int] = {
    for {
      ans <- slowly(if(n == 0) 1.pure[Logged] else factorialWriter(n - 1).map(_ * n))
      _ <- Vector(s"fact $n $ans").tell
    } yield {
      ans
    }
  }

  Await.result(Future.sequence(Vector(
    Future(factorialWriter(3).run.map(t => println(t._1))),
    Future(factorialWriter(3).run.map(t => println(t._1)))
  )), 5.seconds)

  // Reader
  val users = Map(
    1 -> "dade",
    2 -> "kate",
    3 -> "margo"
  )
  val passwords = Map(
    "dade"  -> "zerocool",
    "kate"  -> "acidburn",
    "margo" -> "secret"
  )
  val db = Db(users, passwords)
  val check1 = checkLogin(1, "zerocool").run(db)
  println(s"check1 $check1")
  assert(check1)
  // res10: cats.Id[Boolean] = true
  val check2 = checkLogin(4, "davinci").run(db)
  println(s"check2 $check2")
  assert(!check2)
  // res11: cats.Id[Boolean] = false
}
