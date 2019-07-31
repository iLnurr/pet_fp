package test

import com.iserba.fp.parFIO

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.{higherKinds, implicitConversions}
import Helper._

object IOTest extends App {
  import parFIO._

  val io1 = IO(() => 1)
  val io2 = IO(() => 2)

  val program =
    for {
      res11 <- io1
      res12 <- io1
      res21 <- io2
    } yield res11() + res12() + res21()

  val result = program.runFree.await

  assert(result == 4)
}
object Helper{
  implicit class FtoA[A](val f: Future[A]) extends AnyVal {
    def await: A = Await.result(f, 2.seconds)
  }
}
