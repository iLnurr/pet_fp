package test

import com.iserba.fp.utils.StreamProcessHelper._
import com.iserba.fp._
import com.iserba.fp.utils.Monad.MonadCatch
import com.iserba.fp.utils.Free._
import com.iserba.fp.utils.StreamProcess

import scala.util.Random

object TestStreamProcess extends App {
  implicit val optMC = new MonadCatch[({type f[a] = Option[a]})#f] {
    override def attempt[A](a: Option[A]): Option[Either[Throwable, A]] = a.map{v =>
      Right(v)
    }
    override def fail[A](t: Throwable): Option[A] = None
    override def unit[A](a: => A): Option[A] = Some(a)
    override def flatMap[A, B](a: Option[A])(f: A => Option[B]): Option[B] = a.flatMap(f)
  }

  eval(Option(Random.nextLong())).map(l => println(s"First $l")).runLog

  eval(run(IO(Option(Random.nextLong())))).map(l => println(s"Second $l")).runLog

  def acquire =
    IO(Option(Random.nextLong()))
  def use(l: Option[Long]): StreamProcess[IO,Option[Long]] =
    eval(IO(l))
  def release(l: Option[Long]): StreamProcess[IO,Option[Long]] =
    eval(IO(l))


  resource_(acquire){l => eval(IO(l))}{_ => IO(())}.map(ll => println(s"Third $ll")).runLog

}
