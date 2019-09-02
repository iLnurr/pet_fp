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

  implicit val treeFunctor: Functor[Tree] = new Functor[Tree] {
    override def map[A, B](fa: Tree[A])(f: A => B): Tree[B] = fa match {
      case Branch(left, right) =>
        Branch(map(left)(f), map(right)(f))
      case Leaf(value) =>
        Leaf(f(value))
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

  implicit val treeMonad: Monad[Tree] = new Monad[Tree] {
    override def pure[A](x: A): Tree[A] =
      Tree.leaf(x)

    override def flatMap[A, B](fa: Tree[A])(f: A => Tree[B]): Tree[B] = fa match {
      case Branch(left, right) =>
        Tree.branch(flatMap(left)(f), flatMap(right)(f))
      case Leaf(value) =>
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
                      (func: A => Tree[Either[A, B]]): Tree[B] = {
      @tailrec
      def loop(open: List[Tree[Either[A, B]]],
               closed: List[Option[Tree[B]]]): List[Tree[B]] = open match {
        case Branch(l, r) :: next =>
          loop(l :: r :: next, None :: closed)
        case Leaf(Left(value)) :: next =>
          loop(func(value) :: next, closed)
        case Leaf(Right(value)) :: next =>
          loop(next, Some(pure(value)) :: closed)
        case Nil =>
          closed.foldLeft(Nil: List[Tree[B]]) { (acc, maybeTree) =>
            maybeTree.map(_ :: acc).getOrElse {
              val left :: right :: tail = acc
              Tree.branch(left, right) :: tail
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
}
