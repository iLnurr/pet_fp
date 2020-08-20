package shapelessex.ch6

import shapeless._
import cats.Monoid
import shapeless.labelled.{field, FieldType}
import shapeless.ops._
import shapelessex.ch6.algebra.Migration

object impl {

  def createMonoid[A](zero: A)(add: (A, A) => A): Monoid[A] =
    new Monoid[A] {
      def empty = zero

      def combine(x: A, y: A): A = add(x, y)
    }

  implicit val hnilMonoid: Monoid[HNil] =
    createMonoid[HNil](HNil)((_, _) => HNil)

  implicit def emptyHList[K <: Symbol, H, T <: HList](
    implicit
    hMonoid: Lazy[Monoid[H]],
    tMonoid: Monoid[T]
  ): Monoid[FieldType[K, H] :: T] =
    createMonoid(field[K](hMonoid.value.empty) :: tMonoid.empty) { (x, y) =>
      field[K](hMonoid.value.combine(x.head, y.head)) :: tMonoid
        .combine(x.tail, y.tail)
    }

  implicit def genericMigration[
    A,
    B,
    ARepr <: HList,
    BRepr <: HList,
    Common <: HList,
    Added <: HList,
    Unaligned <: HList
  ](
    implicit
    aGen:    LabelledGeneric.Aux[A, ARepr],
    bGen:    LabelledGeneric.Aux[B, BRepr],
    inter:   hlist.Intersection.Aux[ARepr, BRepr, Common],
    diff:    hlist.Diff.Aux[BRepr, Common, Added],
    monoid:  Monoid[Added],
    prepend: hlist.Prepend.Aux[Added, Common, Unaligned],
    align:   hlist.Align[Unaligned, BRepr]
  ): Migration[A, B] =
    new Migration[A, B] {
      def apply(a: A): B =
        bGen.from(align(prepend(monoid.empty, inter(aGen.to(a)))))
    }

}
