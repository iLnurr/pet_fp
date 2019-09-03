package ch1

import cats.{Functor, Monad}
import ch1.algebra._
import ch1.domain._

import scala.annotation.tailrec

object impl {
  implicit val stringPrintable: Printable[String] =
    new Printable[String] {
      def format(value: String): String =
        "\"" + value + "\""
    }
  implicit val booleanPrintable: Printable[Boolean] = new Printable[Boolean] {
    def format(value: Boolean): String =
      if(value) "yes" else "no"
  }

  implicit val intPrintable: Printable[Int] = new Printable[Int] {
    override def format(a: Int): String =
      a.toString
  }
  implicit val catPrintable: Printable[Cat] = new Printable[Cat] {
    override def format(a: Cat): String =
      s"${Printable.format(a.name)} is a ${Printable.format(a.age)} year-old ${Printable.format(a.color)} cat."
  }

  implicit def boxPrintable[A](implicit p: Printable[A]): Printable[Box[A]] =
    p.contramap[Box[A]](_.value)

  import cats.Show
  import cats.instances.int._
  import cats.instances.string._
  import cats.syntax.show._
  implicit val catShow: Show[Cat] =
    Show.show(cat => s"${cat.name.show} is a ${cat.age.show} year-old ${cat.color.show} cat.")

  import cats.Eq
  import cats.instances.int._
  import cats.instances.string._
  import cats.syntax.eq._
  implicit val catEq: Eq[Cat] =
    Eq.instance[Cat]((c1,c2) => c1.name === c2.name && c1.age === c2.age && c1.color === c2.color)

  implicit val treeFunctor: Functor[BinaryTree] = new Functor[BinaryTree] {
    override def map[A, B](fa: BinaryTree[A])(f: A => B): BinaryTree[B] = fa match {
      case BinaryBranch(left, right) =>
        BinaryBranch(map(left)(f), map(right)(f))
      case BinaryLeaf(value) =>
        BinaryLeaf(f(value))
    }
  }

  implicit val stringCodec: Codec[String] =
    new Codec[String] {
      def encode(value: String): String = value
      def decode(value: String): String = value
    }
  implicit val intCodec: Codec[Int] =
    stringCodec.imap(_.toInt, _.toString)
  implicit val booleanCodec: Codec[Boolean] =
    stringCodec.imap(_.toBoolean, _.toString)
  implicit val doubleCodec: Codec[Double] =
    stringCodec.imap(_.toDouble, _.toString)
  implicit def boxCodec[A](implicit c: Codec[A]): Codec[Box[A]] =
    c.imap(Box.apply, _.value)

  implicit val treeMonad: Monad[BinaryTree] = new Monad[BinaryTree] {
    override def pure[A](x: A): BinaryTree[A] =
      BinaryTree.leaf(x)

    override def flatMap[A, B](fa: BinaryTree[A])(f: A => BinaryTree[B]): BinaryTree[B] = fa match {
      case BinaryBranch(left, right) =>
        BinaryTree.branch(flatMap(left)(f), flatMap(right)(f))
      case BinaryLeaf(value) =>
        f(value)
    }

    // not stack safe
//    override def tailRecM[A, B](a: A)(f: A => Tree[Either[A, B]]): Tree[B] =
//      flatMap(f(a)) {
//        case Left(aa) =>
//          tailRecM(aa)(f)
//        case Right(bb) =>
//          Tree.leaf(bb)
//      }

    // stack safe from https://stackoverflow.com/questions/44504790/cats-non-tail-recursive-tailrecm-method-for-monads
    override def tailRecM[A, B](arg: A)
                      (func: A => BinaryTree[Either[A, B]]): BinaryTree[B] = {
      @tailrec
      def loop(open: List[BinaryTree[Either[A, B]]],
               closed: List[Option[BinaryTree[B]]]): List[BinaryTree[B]] = open match {
        case BinaryBranch(l, r) :: next =>
          loop(l :: r :: next, None :: closed)
        case BinaryLeaf(Left(value)) :: next =>
          loop(func(value) :: next, closed)
        case BinaryLeaf(Right(value)) :: next =>
          loop(next, Some(pure(value)) :: closed)
        case Nil =>
          closed.foldLeft(Nil: List[BinaryTree[B]]) { (acc, maybeTree) =>
            maybeTree.map(_ :: acc).getOrElse {
              val left :: right :: tail = acc
              BinaryTree.branch(left, right) :: tail
            }
          }
      }
      loop(List(func(arg)), Nil).head
    }
  }

  // Implement product in terms of flatMap:
  import cats.Monad
  import cats.syntax.flatMap._ // for flatMap
  import cats.syntax.functor._ // for map
  def product11[M[_]: Monad, A, B](x: M[A], y: M[B]): M[(A, B)] =
    Monad[M].flatMap(x)(a => Monad[M].map(y)(b => (a,b)))
  def product12[M[_]: Monad, A, B](x: M[A], y: M[B]): M[(A, B)] =
    x.flatMap(a => y.map(b => (a,b)))
  def product13[M[_]: Monad, A, B](x: M[A], y: M[B]): M[(A, B)] =
    for {
      a <- x
      b <- y
    } yield (a,b)

  // 7.1.2
  //Try using foldLeft and foldRight with an empty list as the accumulator and :: as the binary operator.
  // What results do you get in each case?
  def fl[T](l: List[T]): List[T] =
    l.foldLeft(List.empty[T])((acc,t) => t :: acc)
  def fr[T](l: List[T]): List[T] =
    l.foldRight(List.empty[T])(_ :: _)
  //foldLeft and foldRight are very general methods.
  // We can use them to implement many of the other high-level sequence opera􏰀ons we know.
  // Prove this to yourself by implemen􏰀ng subs􏰀tutes for List's
  // map, flatMap, filter, and sum methods
  // in terms of foldRight.
  def map[A,B](l: List[A])(f: A => B): List[B] =
    l.foldRight(List.empty[B])(f(_) :: _)
  def flatMap[A,B](l: List[A])(f: A => List[B]): List[B] =
    l.foldRight(List.empty[B])(f(_) ++ _)
  def filter[A](l: List[A])(f: A => Boolean): List[A] =
    l.foldRight(List.empty[A])((a,acc) => if (f(a)) a :: acc else acc)


}
