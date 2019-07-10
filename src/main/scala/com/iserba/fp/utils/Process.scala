package com.iserba.fp.utils

import com.iserba.fp.utils.Monad.MonadCatch
import com.iserba.fp.utils.Process.{Channel, Process1, Sink, Tee}

import language.implicitConversions
import language.higherKinds
import language.postfixOps

trait Process[F[_],O] extends Monad[F] {
  def ++(p: => Process[F,O]): Process[F,O]
  /*
     * Like `++`, but _always_ runs `p`, even if `this` halts with an error.
     */
  def onComplete(p: => Process[F,O]): Process[F,O]
  def asFinalizer: Process[F,O]
  def onHalt(f: Throwable => Process[F,O]): Process[F,O]
  def runLog(implicit F: MonadCatch[F]): F[IndexedSeq[O]]
  /*
       * We define `Process1` as a type alias - see the companion object
       * for `Process` below. Using that, we can then define `|>` once
       * more. The definition is extremely similar to our previous
       * definition. We again use the helper function, `feed`, to take
       * care of the case where `this` is emitting values while `p2`
       * is awaiting these values.
       *
       * The one subtlety is we make sure that if `p2` halts, we
       * `kill` this process, giving it a chance to run any cleanup
       * actions (like closing file handles, etc).
       */
  def |>[O2](p2: Process1[O,O2]): Process[F,O2]
  /** Alias for `this |> p2`. */
  def pipe[O2](p2: Process1[O,O2]): Process[F,O2] =
    this |> p2

  @annotation.tailrec
  def kill[O2]: Process[F,O2]
  def drain[O2]: Process[F,O2]

  /*
     * Use a `Tee` to interleave or combine the outputs of `this` and
     * `p2`. This can be used for zipping, interleaving, and so forth.
     * Nothing requires that the `Tee` read elements from each
     * `Process` in lockstep. It could read fifty elements from one
     * side, then two elements from the other, then combine or
     * interleave these values in some way, etc.
     *
     * This definition uses two helper functions, `feedL` and `feedR`,
     * which feed the `Tee` in a tail-recursive loop as long as
     * it is awaiting input.
     */
  def tee[O2,O3](p2: Process[F,O2])(t: Tee[O,O2,O3]): Process[F,O3]
  def to[O2](sink: Sink[F,O]): Process[F,Unit]
  def through[O2](p2: Channel[F, O, O2]): Process[F,O2]
}
object Process {
  case class Is[I]() {
    sealed trait f[X]
    val Get = new f[I] {}
  }
  def Get[I] = Is[I]().Get

  type Process1[I,O] = Process[Is[I]#f, O]

  /*
      We sometimes need to construct a `Process` that will pull values
      from multiple input sources. For instance, suppose we want to
      'zip' together two files, `f1.txt` and `f2.txt`, combining
      corresponding lines in some way. Using the same trick we used for
      `Process1`, we can create a two-input `Process` which can request
      values from either the 'left' stream or the 'right' stream. We'll
      call this a `Tee`, after the letter 'T', which looks like a
      little diagram of two inputs being combined into one output.
     */

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