package com.iserba.fp.utils

import com.iserba.fp.utils.Monad.MonadCatch
import com.iserba.fp.utils.Process._
import Helper._

import language.implicitConversions
import language.higherKinds
import language.postfixOps

trait Process[F[_],O] {
  def ++(p: => Process[F,O]): Process[F,O] = this.onHalt {
    case End => Try(p)
    case err => Halt(err)
  }

  def flatMap[O2](f: O => Process[F,O2]): Process[F,O2] =
    this match {
      case Halt(err) => Halt(err)
      case Emit(o, t) => Try(f(o)) ++ t.flatMap(f)
      case Await(req,recv) =>
        Await(req, recv andThen (_ flatMap f))
    }
  def join[A](p: Process[F,Process[F,A]]): Process[F,A] =
    p.flatMap(pp => pp)
  /*
     * Like `++`, but _always_ runs `p`, even if `this` halts with an error.
     */
  def onComplete(p: => Process[F,O]): Process[F,O] =
    this.onHalt {
      case End => p.asFinalizer
      case err => p.asFinalizer ++ Halt(err) // we always run `p`, but preserve any errors
    }

  def asFinalizer: Process[F,O] = this match {
    case Emit(h, t) => Emit(h, t.asFinalizer)
    case Halt(e) => Halt(e)
    case Await(req,recv) => await(req) {
      case Left(Kill) => this.asFinalizer
      case x => recv(x)
    }
  }

  def onHalt(f: Throwable => Process[F,O]): Process[F,O] = this match {
    case Halt(e) => Try(f(e))
    case Emit(h, t) => Emit(h, t.onHalt(f))
    case Await(req,recv) => Await(req, recv andThen (_.onHalt(f)))
  }

  def runLog(implicit F: MonadCatch[F]): F[IndexedSeq[O]] = {
    def go(cur: Process[F,O], acc: IndexedSeq[O]): F[IndexedSeq[O]] =
      cur match {
        case Emit(h,t) => go(t, acc :+ h)
        case Halt(End) => F.unit(acc)
        case Halt(err) => F.fail(err)
        case Await(req,recv) => F.flatMap (F.attempt(req)) { e => go(Try(recv(e)), acc) }
      }
    go(this, IndexedSeq())
  }

  def |>[O2](p2: Process1[O,O2]): Process[F,O2] = {
    p2 match {
      case Halt(e) => this.kill onHalt { e2 => Halt(e) ++ Halt(e2) }
      case Emit(h, t) => Emit(h, this |> t)
      case Await(req,recv) => this match {
        case Halt(err) => Halt(err) |> recv(Left(err))
        case Emit(h,t) => t |> Try(recv(Right(h)))
        case Await(req0,recv0) => await(req0)(recv0 andThen (_ |> p2))
      }
    }
  }
  def pipe[O2](p2: Process1[O,O2]): Process[F,O2] =
    this |> p2

  @annotation.tailrec
  final def kill[O2]: Process[F,O2] = this match {
    case Await(req,recv) => recv(Left(Kill)).drain.onHalt {
      case Kill => Halt(End) // we convert the `Kill` exception back to normal termination
      case e => Halt(e)
    }
    case Halt(e) => Halt(e)
    case Emit(h, t) => t.kill
  }
  final def drain[O2]: Process[F,O2] = this match {
    case Halt(e) => Halt(e)
    case Emit(h, t) => t.drain
    case Await(req,recv) => Await(req, recv andThen (_.drain))
  }

  def tee[O2,O3](p2: Process[F,O2])(t: Tee[O,O2,O3]): Process[F,O3] = {
    t match {
      case Halt(e) => this.kill onComplete p2.kill onComplete Halt(e)
      case Emit(h,t) => Emit(h, (this tee p2)(t))
      case Await(side, recv) => side.get match {
        case Left(isO) => this match {
          case Halt(e) => p2.kill onComplete Halt(e)
          case Emit(o,ot) => (ot tee p2)(Try(recv(Right(o))))
          case Await(reqL, recvL) =>
            await(reqL)(recvL andThen (this2 => this2.tee(p2)(t)))
        }
        case Right(isO2) => p2 match {
          case Halt(e) => this.kill onComplete Halt(e)
          case Emit(o2,ot) => (this tee ot)(Try(recv(Right(o2))))
          case Await(reqR, recvR) =>
            await(reqR)(recvR andThen (p3 => this.tee(p3)(t)))
        }
      }
    }
  }

  def repeat: Process[F,O] =
    this ++ this.repeat

  def zipWith[O2,O3](p2: Process[F,O2])(f: (O,O2) => O3): Process[F,O3] =
    (this tee p2)(Helper.zipWith(f))

  def zip[O2](p2: Process[F,O2]): Process[F,(O,O2)] =
    zipWith(p2)((_,_))

  def to[O2](sink: Sink[F,O]): Process[F,Unit] =
    join { (this zipWith sink)((o,f) => f(o)) }

  def through[O2](p2: Channel[F, O, O2]): Process[F,O2] =
    join { (this zipWith p2)((o,f) => f(o)) }
}

object Process {
  case class Await[F[_],A,O](req: F[A], recv: Either[Throwable,A] => Process[F,O]) extends Process[F,O]
  case class Emit[F[_],O](head: O, tail: Process[F,O]) extends Process[F,O]
  case class Halt[F[_],O](err: Throwable) extends Process[F,O]

  case class Is[I]() {
    sealed trait f[X]
    val Get = new f[I] {}
  }
  def Get[I] = Is[I]().Get
  type Process1[I,O] = Process[Is[I]#f, O]

  case class T[I,I2]() {
    sealed trait f[X] { def get: Either[I => X, I2 => X] }
    val L = new f[I] { def get = Left(identity) }
    val R = new f[I2] { def get = Right(identity) }
  }
  def L[I,I2] = T[I,I2]().L
  def R[I,I2] = T[I,I2]().R
  type Tee[I,I2,O] = Process[T[I,I2]#f, O]

  type Sink[F[_],O] = Process[F, O => Process[F,Unit]]

  type Channel[F[_],I,O] = Process[F, I => Process[F,O]]
}

object Helper {
  def Try[F[_],O](p: => Process[F,O]): Process[F,O] =
    try p
    catch { case e: Throwable => Halt(e) }

  /* Special exception indicating normal termination */
  case object End extends Exception
  /* Special exception indicating forceful termination */
  case object Kill extends Exception

  /** PROCESS */
  def emit[F[_],O](head: O,
                   tail: Process[F,O] = Halt[F,O](End)): Process[F,O] =
    Emit(head, tail)

  def await[F[_],A,O](req: F[A])(recv: Either[Throwable,A] => Process[F,O]): Process[F,O] =
    Await(req, recv)

  def eval[F[_],A](a: F[A]): Process[F,A] =
    await(a) {
      case Left(err) => Halt(err)
      case Right(value) => Emit[F,A](value, Halt(End))
    }


  /** PROCESS1 */
  def await1[I,O](
                   recv: I => Process1[I,O],
                   fallback: => Process1[I,O] = halt1[I,O]): Process1[I, O] =
    Await(Get[I], (e: Either[Throwable,I]) => e match {
      case Left(End) => fallback
      case Left(err) => Halt(err)
      case Right(i) => Try(recv(i))
    })

  def emit1[I,O](h: O, tl: Process1[I,O] = halt1[I,O]): Process1[I,O] =
    emit(h, tl)

  def halt1[I,O]: Process1[I,O] = Halt[Is[I]#f, O](End)

  def lift[I,O](f: I => O): Process1[I,O] =
    await1[I,O]((i:I) => emit(f(i))) repeat

  def filter[I](f: I => Boolean): Process1[I,I] =
    await1[I,I](i => if (f(i)) emit(i) else halt1) repeat

  // we can define take, takeWhile, and so on as before

  def take[I](n: Int): Process1[I,I] =
    if (n <= 0) halt1
    else await1[I,I](i => emit(i, take(n-1)))

  def takeWhile[I](f: I => Boolean): Process1[I,I] =
    await1(i =>
      if (f(i)) emit(i, takeWhile(f))
      else      halt1)

  def dropWhile[I](f: I => Boolean): Process1[I,I] =
    await1(i =>
      if (f(i)) dropWhile(f)
      else      emit(i,id))

  def id[I]: Process1[I,I] =
    await1((i: I) => emit(i, id))

  def window2[I]: Process1[I,(Option[I],I)] = {
    def go(prev: Option[I]): Process1[I,(Option[I],I)] =
      await1[I,(Option[I],I)](i => emit(prev -> i) ++ go(Some(i)))
    go(None)
  }

  /* Emits `sep` in between each input received. */
  def intersperse[I](sep: I): Process1[I,I] =
    await1[I,I](i => emit1(i) ++ id.flatMap(i => emit1(sep) ++ emit1(i)))


  /** TEE */
  def haltT[I,I2,O]: Tee[I,I2,O] =
    Halt[T[I,I2]#f,O](End)

  def awaitL[I,I2,O](recv: I => Tee[I,I2,O],
                     fallback: => Tee[I,I2,O] = haltT[I,I2,O]): Tee[I,I2,O] =
    await[T[I,I2]#f,I,O](L) {
      case Left(End) => fallback
      case Left(err) => Halt(err)
      case Right(a) => Try(recv(a))
    }

  def awaitR[I,I2,O](recv: I2 => Tee[I,I2,O],
                     fallback: => Tee[I,I2,O] = haltT[I,I2,O]): Tee[I,I2,O] =
    await[T[I,I2]#f,I2,O](R) {
      case Left(End) => fallback
      case Left(err) => Halt(err)
      case Right(a) => Try(recv(a))
    }

  def emitT[I,I2,O](h: O, tl: Tee[I,I2,O] = haltT[I,I2,O]): Tee[I,I2,O] =
    emit(h, tl)

  def zipWith[I,I2,O](f: (I,I2) => O): Tee[I,I2,O] =
    awaitL[I,I2,O](i  =>
      awaitR        (i2 => emitT(f(i,i2)))) repeat

  def zip[I,I2]: Tee[I,I2,(I,I2)] = zipWith((_,_))

  /* Ignores all input from left. */
  def passR[I,I2]: Tee[I,I2,I2] = awaitR(emitT(_, passR))

  /* Ignores input from the right. */
  def passL[I,I2]: Tee[I,I2,I] = awaitL(emitT(_, passL))

  /* Alternate pulling values from the left and the right inputs. */
  def interleaveT[I]: Tee[I,I,I] =
    awaitL[I,I,I](i =>
      awaitR       (i2 => emitT(i) ++ emitT(i2))) repeat
}