package test

import com.iserba.fp.utils.StreamProcessHelper._
import com.iserba.fp.{parFIO, _}
import com.iserba.fp.utils.Monad.MonadCatch
import com.iserba.fp.utils.Free._
import com.iserba.fp.utils.{Free, StreamProcess}

import scala.util.Random

object TestStreamProcess extends App {
  import parFIO._
  implicit val optMC: MonadCatch[Option] = new MonadCatch[({type f[a] = Option[a]})#f] {
    override def attempt[A](a: Option[A]): Option[Either[Throwable, A]] = a.map{v =>
      Right(v)
    }
    override def fail[A](t: Throwable): Option[A] = None
    override def unit[A](a: => A): Option[A] = Some(a)
    override def flatMap[A, B](a: Option[A])(f: A => Option[B]): Option[B] = a.flatMap(f)
  }

  eval(Option(Random.nextLong())).map(l => println(s"First $l")).runStreamProcess

  eval(run(IO(Option(Random.nextLong())))).map(l => println(s"Second $l")).runStreamProcess

  def acquire: IO[Option[Long]] =
    IO(Option(Random.nextLong()))
  def use(l: Option[Long]): StreamProcess[IO,Option[Long]] =
    eval(IO{
      println(s"Third $l")
      l
    })
  def release(l: Option[Long]): StreamProcess[IO,Option[Long]] =
    eval(IO(l))


  Free.run( // end of ev
    resource(acquire){l => use(l)}{l => release(l)}.runStreamProcess // IO streams
  )

}
