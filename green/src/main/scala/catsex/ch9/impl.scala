package catsex.ch9

import cats.{Applicative, Monoid}

import scala.concurrent.{ExecutionContext, Future}

object impl {
  def foldMap[A, B: Monoid](vector: Vector[A])(f: A => B): B =
    vector
      .map(f)
      .fold(Monoid.empty[B])(Monoid.combine[B])

  //split the work into batches, one batch per CPU. Process each batch in a parallel thread.
  //For bonus points, process the batches for each CPU using your implementation of foldMap from above.
  def parallelFoldMap1[A, B: Monoid](
    values: Vector[A]
  )(func:   A => B)(implicit ec: ExecutionContext): Future[B] = {
    val cpus:    Int                 = Runtime.getRuntime.availableProcessors
    val batches: Iterator[Vector[A]] = values.grouped(cpus)
    val mapped: Iterator[Future[B]] =
      batches.map(vector => Future(foldMap(vector)(func)))
    val reduced: Future[B] =
      Future.sequence(mapped).map(b => foldMap(b.toVector)(identity))
    reduced
  }

  import cats.instances.vector._
  import cats.syntax.foldable._
  import cats.syntax.functor._
  import cats.syntax.traverse._
  //Reimplement parallelFoldMap using Cats’ Foldable and Traverseable type classes.
  def parallelFoldMap2[F[_]: Applicative, A, B: Monoid](
    values: Vector[A]
  )(func:   A => B): F[B] = {
    val cpus    = Runtime.getRuntime.availableProcessors
    val batches = values.grouped(cpus).toVector
    val mapped: F[Vector[B]] =
      batches.traverse(batch => Applicative[F].pure(batch.foldMap(func)))
    val reduced: F[B] = mapped.map(_.combineAll)
    reduced
  }
}
