
package scalan.arrays

import scala.annotation.implicitNotFound
import scala.annotation.unchecked.uncheckedVariance
import scalan._
import scalan.common.Default
import scala.reflect.runtime.universe._
import scalan.common.Default.defaultVal


trait PArraysAbs extends PArrays
{ self: PArraysDsl =>

  // single proxy for each type family
  implicit def proxyPArray[A:Elem](p: PA[A]): PArray[A] = {
    proxyOps[PArray[A]](p)
  }

  trait PArrayElem[From,To] extends ViewElem[From, To]

  trait PArrayCompanionElem extends CompanionElem[PArrayCompanionAbs]
  implicit lazy val PArrayCompanionElem: PArrayCompanionElem = new PArrayCompanionElem {
    lazy val tag = typeTag[PArrayCompanionAbs]
    lazy val defaultRep = defaultVal(PArray)
  }

  trait PArrayCompanionAbs extends PArrayCompanionOps
  def PArray: Rep[PArrayCompanionAbs]
  implicit def defaultOfPArray[A:Elem]: Default[Rep[PArray[A]]] = PArray.defaultOf[A]
  implicit def proxyPArrayCompanion(p: Rep[PArrayCompanionOps]): PArrayCompanionOps = {
    proxyOps[PArrayCompanionOps](p, Some(true))
  }


  // elem for concrete class
  trait BaseArrayElem[A] extends PArrayElem[BaseArrayData[A], BaseArray[A]]

  // state representation type
  type BaseArrayData[A] = Array[A]

  // 3) Iso for concrete class
  abstract class BaseArrayIso[A](implicit eA: Elem[A])
    extends IsoBase[BaseArrayData[A], BaseArray[A]] {
    override def from(p: Rep[BaseArray[A]]) =
      unmkBaseArray(p) match {
        case Some((arr)) => arr
        case None => !!!
      }
    override def to(p: Rep[Array[A]]) = {
      val arr = p
      BaseArray(arr)
    }
    lazy val tag = {
      implicit val tagA = element[A].tag
      typeTag[BaseArray[A]]
    }
    lazy val defaultRepTo = defaultVal[Rep[BaseArray[A]]](BaseArray(element[Array[A]].defaultRepValue))
  }
  // 4) constructor and deconstructor
  trait BaseArrayCompanionAbs extends BaseArrayCompanionOps {

    def apply[A]
          (arr: Rep[Array[A]])
          (implicit eA: Elem[A]): Rep[BaseArray[A]]
        = mkBaseArray(arr)
    def unapply[A:Elem](p: Rep[BaseArray[A]]) = unmkBaseArray(p)
  }

  def BaseArray: Rep[BaseArrayCompanionAbs]
  implicit def proxyBaseArrayCompanion(p: Rep[BaseArrayCompanionAbs]): BaseArrayCompanionAbs = {
    proxyOps[BaseArrayCompanionAbs](p, Some(true))
  }

  trait BaseArrayCompanionElem extends CompanionElem[BaseArrayCompanionAbs]
  implicit lazy val BaseArrayCompanionElem: BaseArrayCompanionElem = new BaseArrayCompanionElem {
    lazy val tag = typeTag[BaseArrayCompanionAbs]
    lazy val defaultRep = defaultVal(BaseArray)
  }

  implicit def proxyBaseArray[A:Elem](p: Rep[BaseArray[A]]): BaseArrayOps[A] = {
    proxyOps[BaseArrayOps[A]](p)
  }

  implicit class ExtendedBaseArray[A](p: Rep[BaseArray[A]])(implicit eA: Elem[A]) {
    def toData: Rep[BaseArrayData[A]] = isoBaseArray(eA).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoBaseArray[A](implicit eA: Elem[A]): Iso[BaseArrayData[A], BaseArray[A]]

  // 6) smart constructor and deconstructor
  def mkBaseArray[A](arr: Rep[Array[A]])(implicit eA: Elem[A]): Rep[BaseArray[A]]
  def unmkBaseArray[A:Elem](p: Rep[BaseArray[A]]): Option[(Rep[Array[A]])]


  // elem for concrete class
  trait PairArrayElem[A, B] extends PArrayElem[PairArrayData[A, B], PairArray[A, B]]

  // state representation type
  type PairArrayData[A, B] = (PArray[A], PArray[B])

  // 3) Iso for concrete class
  abstract class PairArrayIso[A, B](implicit eA: Elem[A], eB: Elem[B])
    extends IsoBase[PairArrayData[A, B], PairArray[A, B]] {
    override def from(p: Rep[PairArray[A, B]]) =
      unmkPairArray(p) match {
        case Some((as, bs)) => Pair(as, bs)
        case None => !!!
      }
    override def to(p: Rep[(PArray[A], PArray[B])]) = {
      val Pair(as, bs) = p
      PairArray(as, bs)
    }
    lazy val tag = {
      implicit val tagA = element[A].tag
      implicit val tagB = element[B].tag
      typeTag[PairArray[A, B]]
    }
    lazy val defaultRepTo = defaultVal[Rep[PairArray[A, B]]](PairArray(element[PArray[A]].defaultRepValue, element[PArray[B]].defaultRepValue))
  }
  // 4) constructor and deconstructor
  trait PairArrayCompanionAbs extends PairArrayCompanionOps {

    def apply[A, B](p: Rep[PairArrayData[A, B]])(implicit eA: Elem[A], eB: Elem[B]): Rep[PairArray[A, B]]
        = isoPairArray(eA, eB).to(p)
    def apply[A, B]
          (as: Rep[PArray[A]], bs: Rep[PArray[B]])
          (implicit eA: Elem[A], eB: Elem[B]): Rep[PairArray[A, B]]
        = mkPairArray(as, bs)
    def unapply[A:Elem, B:Elem](p: Rep[PairArray[A, B]]) = unmkPairArray(p)
  }

  def PairArray: Rep[PairArrayCompanionAbs]
  implicit def proxyPairArrayCompanion(p: Rep[PairArrayCompanionAbs]): PairArrayCompanionAbs = {
    proxyOps[PairArrayCompanionAbs](p, Some(true))
  }

  trait PairArrayCompanionElem extends CompanionElem[PairArrayCompanionAbs]
  implicit lazy val PairArrayCompanionElem: PairArrayCompanionElem = new PairArrayCompanionElem {
    lazy val tag = typeTag[PairArrayCompanionAbs]
    lazy val defaultRep = defaultVal(PairArray)
  }

  implicit def proxyPairArray[A:Elem, B:Elem](p: Rep[PairArray[A, B]]): PairArrayOps[A, B] = {
    proxyOps[PairArrayOps[A, B]](p)
  }

  implicit class ExtendedPairArray[A, B](p: Rep[PairArray[A, B]])(implicit eA: Elem[A], eB: Elem[B]) {
    def toData: Rep[PairArrayData[A, B]] = isoPairArray(eA, eB).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoPairArray[A, B](implicit eA: Elem[A], eB: Elem[B]): Iso[PairArrayData[A, B], PairArray[A, B]]

  // 6) smart constructor and deconstructor
  def mkPairArray[A, B](as: Rep[PArray[A]], bs: Rep[PArray[B]])(implicit eA: Elem[A], eB: Elem[B]): Rep[PairArray[A, B]]
  def unmkPairArray[A:Elem, B:Elem](p: Rep[PairArray[A, B]]): Option[(Rep[PArray[A]], Rep[PArray[B]])]

}


trait PArraysSeq extends PArraysAbs
{ self: ScalanSeq with PArraysDsl =>

  lazy val PArray: Rep[PArrayCompanionAbs] = new PArrayCompanionAbs with UserTypeSeq[PArrayCompanionAbs, PArrayCompanionAbs] {
    lazy val selfType = element[PArrayCompanionAbs]
  }

  case class SeqBaseArray[A]
      (override val arr: Rep[Array[A]])
      (implicit override val eA: Elem[A])
    extends BaseArray[A](arr) with UserTypeSeq[PArray[A], BaseArray[A]] {
    lazy val selfType = element[BaseArray[A]].asInstanceOf[Elem[PArray[A]]]
  }

  lazy val BaseArray = new BaseArrayCompanionAbs with UserTypeSeq[BaseArrayCompanionAbs, BaseArrayCompanionAbs] {
    lazy val selfType = element[BaseArrayCompanionAbs]
  }



  implicit def isoBaseArray[A](implicit eA: Elem[A]):Iso[BaseArrayData[A], BaseArray[A]]
    = new BaseArrayIso[A] with SeqIso[BaseArrayData[A], BaseArray[A]] { i =>
        // should use i as iso reference
        override lazy val eTo = new SeqViewElem[BaseArrayData[A], BaseArray[A]]
                                    with BaseArrayElem[A] { val iso = i }
      }


  def mkBaseArray[A]
      (arr: Rep[Array[A]])
      (implicit eA: Elem[A])
      = new SeqBaseArray[A](arr)
  def unmkBaseArray[A:Elem](p: Rep[BaseArray[A]])
    = Some((p.arr))


  case class SeqPairArray[A, B]
      (override val as: Rep[PArray[A]], override val bs: Rep[PArray[B]])
      (implicit override val eA: Elem[A], override val eB: Elem[B])
    extends PairArray[A, B](as, bs) with UserTypeSeq[PArray[(A,B)], PairArray[A, B]] {
    lazy val selfType = element[PairArray[A, B]].asInstanceOf[Elem[PArray[(A,B)]]]
  }

  lazy val PairArray = new PairArrayCompanionAbs with UserTypeSeq[PairArrayCompanionAbs, PairArrayCompanionAbs] {
    lazy val selfType = element[PairArrayCompanionAbs]
  }



  implicit def isoPairArray[A, B](implicit eA: Elem[A], eB: Elem[B]):Iso[PairArrayData[A, B], PairArray[A, B]]
    = new PairArrayIso[A, B] with SeqIso[PairArrayData[A, B], PairArray[A, B]] { i =>
        // should use i as iso reference
        override lazy val eTo = new SeqViewElem[PairArrayData[A, B], PairArray[A, B]]
                                    with PairArrayElem[A, B] { val iso = i }
      }


  def mkPairArray[A, B]
      (as: Rep[PArray[A]], bs: Rep[PArray[B]])
      (implicit eA: Elem[A], eB: Elem[B])
      = new SeqPairArray[A, B](as, bs)
  def unmkPairArray[A:Elem, B:Elem](p: Rep[PairArray[A, B]])
    = Some((p.as, p.bs))

}


trait PArraysExp extends PArraysAbs with scalan.ProxyExp with scalan.ViewsExp
{ self: ScalanStaged with PArraysDsl =>

  lazy val PArray: Rep[PArrayCompanionAbs] = new PArrayCompanionAbs with UserTypeExp[PArrayCompanionAbs, PArrayCompanionAbs] {
    lazy val selfType = element[PArrayCompanionAbs]
    override def mirror(t: Transformer) = this
  }

  case class ExpBaseArray[A]
      (override val arr: Rep[Array[A]])
      (implicit override val eA: Elem[A])
    extends BaseArray[A](arr) with UserTypeExp[PArray[A], BaseArray[A]] {
    lazy val selfType = element[BaseArray[A]].asInstanceOf[Elem[PArray[A]]]
    override def mirror(t: Transformer) = ExpBaseArray[A](t(arr))
  }

  lazy val BaseArray: Rep[BaseArrayCompanionAbs] = new BaseArrayCompanionAbs with UserTypeExp[BaseArrayCompanionAbs, BaseArrayCompanionAbs] {
    lazy val selfType = element[BaseArrayCompanionAbs]
    override def mirror(t: Transformer) = this
  }

  addUserType[ExpBaseArray[_]]


  def mkBaseArray[A]
      (arr: Rep[Array[A]])
      (implicit eA: Elem[A])
      = new ExpBaseArray[A](arr)
  def unmkBaseArray[A:Elem](p: Rep[BaseArray[A]])
    = Some((p.arr))


  implicit def isoBaseArray[A](implicit eA: Elem[A]):Iso[BaseArrayData[A], BaseArray[A]]
    = new BaseArrayIso[A] with StagedIso[BaseArrayData[A], BaseArray[A]] { i =>
        // should use i as iso reference
        override lazy val eTo = new StagedViewElem[BaseArrayData[A], BaseArray[A]]
                                    with BaseArrayElem[A] { val iso = i }
      }


  case class ExpPairArray[A, B]
      (override val as: Rep[PArray[A]], override val bs: Rep[PArray[B]])
      (implicit override val eA: Elem[A], override val eB: Elem[B])
    extends PairArray[A, B](as, bs) with UserTypeExp[PArray[(A,B)], PairArray[A, B]] {
    lazy val selfType = element[PairArray[A, B]].asInstanceOf[Elem[PArray[(A,B)]]]
    override def mirror(t: Transformer) = ExpPairArray[A, B](t(as), t(bs))
  }

  lazy val PairArray: Rep[PairArrayCompanionAbs] = new PairArrayCompanionAbs with UserTypeExp[PairArrayCompanionAbs, PairArrayCompanionAbs] {
    lazy val selfType = element[PairArrayCompanionAbs]
    override def mirror(t: Transformer) = this
  }

  addUserType[ExpPairArray[_, _]]


  def mkPairArray[A, B]
      (as: Rep[PArray[A]], bs: Rep[PArray[B]])
      (implicit eA: Elem[A], eB: Elem[B])
      = new ExpPairArray[A, B](as, bs)
  def unmkPairArray[A:Elem, B:Elem](p: Rep[PairArray[A, B]])
    = Some((p.as, p.bs))


  implicit def isoPairArray[A, B](implicit eA: Elem[A], eB: Elem[B]):Iso[PairArrayData[A, B], PairArray[A, B]]
    = new PairArrayIso[A, B] with StagedIso[PairArrayData[A, B], PairArray[A, B]] { i =>
        // should use i as iso reference
        override lazy val eTo = new StagedViewElem[PairArrayData[A, B], PairArray[A, B]]
                                    with PairArrayElem[A, B] { val iso = i }
      }

}
