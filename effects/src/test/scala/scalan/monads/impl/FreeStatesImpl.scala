package scalan.monads

import scalan._
import scala.reflect.runtime.universe._
import scalan.collections.CollectionsDslExp
import scala.reflect.runtime.universe.{WeakTypeTag, weakTypeTag}
import scalan.meta.ScalanAst._

package impl {
// Abs -----------------------------------
trait FreeStatesAbs extends scalan.ScalanDsl with FreeStates {
  self: MonadsDsl =>

  // single proxy for each type family
  implicit def proxyStateF[S, A](p: Rep[StateF[S, A]]): StateF[S, A] = {
    proxyOps[StateF[S, A]](p)(scala.reflect.classTag[StateF[S, A]])
  }

  // familyElem
  class StateFElem[S, A, To <: StateF[S, A]](implicit _eS: Elem[S], _eA: Elem[A])
    extends EntityElem[To] {
    def eS = _eS
    def eA = _eA
    lazy val parent: Option[Elem[_]] = None
    lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("S" -> Left(eS), "A" -> Left(eA))
    }
    override def isEntityType = true
    override lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagA = eA.tag
      weakTypeTag[StateF[S, A]].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      implicit val eTo: Elem[To] = this
      val conv = fun {x: Rep[StateF[S, A]] => convertStateF(x) }
      tryConvert(element[StateF[S, A]], this, x, conv)
    }

    def convertStateF(x: Rep[StateF[S, A]]): Rep[To] = {
      x.selfType1 match {
        case _: StateFElem[_, _, _] => x.asRep[To]
        case e => !!!(s"Expected $x to have StateFElem[_, _, _], but got $e", x)
      }
    }

    override def getDefaultRep: Rep[To] = ???
  }

  implicit def stateFElement[S, A](implicit eS: Elem[S], eA: Elem[A]): Elem[StateF[S, A]] =
    cachedElem[StateFElem[S, A, StateF[S, A]]](eS, eA)

  implicit case object StateFCompanionElem extends CompanionElem[StateFCompanionAbs] {
    lazy val tag = weakTypeTag[StateFCompanionAbs]
    protected def getDefaultRep = StateF
  }

  abstract class StateFCompanionAbs extends CompanionDef[StateFCompanionAbs] with StateFCompanion {
    def selfType = StateFCompanionElem
    override def toString = "StateF"
  }
  def StateF: Rep[StateFCompanionAbs]
  implicit def proxyStateFCompanionAbs(p: Rep[StateFCompanionAbs]): StateFCompanionAbs =
    proxyOps[StateFCompanionAbs](p)

  abstract class AbsStateGet[S, A]
      (f: Rep[S => A])(implicit eS: Elem[S], eA: Elem[A])
    extends StateGet[S, A](f) with Def[StateGet[S, A]] {
    lazy val selfType = element[StateGet[S, A]]
  }
  // elem for concrete class
  class StateGetElem[S, A](val iso: Iso[StateGetData[S, A], StateGet[S, A]])(implicit override val eS: Elem[S], override val eA: Elem[A])
    extends StateFElem[S, A, StateGet[S, A]]
    with ConcreteElem[StateGetData[S, A], StateGet[S, A]] {
    override lazy val parent: Option[Elem[_]] = Some(stateFElement(element[S], element[A]))
    override lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("S" -> Left(eS), "A" -> Left(eA))
    }

    override def convertStateF(x: Rep[StateF[S, A]]) = // Converter is not generated by meta
!!!("Cannot convert from StateF to StateGet: missing fields List(f)")
    override def getDefaultRep = StateGet(constFun[S, A](element[A].defaultRepValue))
    override lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagA = eA.tag
      weakTypeTag[StateGet[S, A]]
    }
  }

  // state representation type
  type StateGetData[S, A] = S => A

  // 3) Iso for concrete class
  class StateGetIso[S, A](implicit eS: Elem[S], eA: Elem[A])
    extends EntityIso[StateGetData[S, A], StateGet[S, A]] with Def[StateGetIso[S, A]] {
    override def from(p: Rep[StateGet[S, A]]) =
      p.f
    override def to(p: Rep[S => A]) = {
      val f = p
      StateGet(f)
    }
    lazy val eFrom = element[S => A]
    lazy val eTo = new StateGetElem[S, A](self)
    lazy val selfType = new StateGetIsoElem[S, A](eS, eA)
    def productArity = 2
    def productElement(n: Int) = (eS, eA).productElement(n)
  }
  case class StateGetIsoElem[S, A](eS: Elem[S], eA: Elem[A]) extends Elem[StateGetIso[S, A]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new StateGetIso[S, A]()(eS, eA))
    lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagA = eA.tag
      weakTypeTag[StateGetIso[S, A]]
    }
  }
  // 4) constructor and deconstructor
  class StateGetCompanionAbs extends CompanionDef[StateGetCompanionAbs] with StateGetCompanion {
    def selfType = StateGetCompanionElem
    override def toString = "StateGet"

    @scalan.OverloadId("fromFields")
    def apply[S, A](f: Rep[S => A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StateGet[S, A]] =
      mkStateGet(f)

    def unapply[S, A](p: Rep[StateF[S, A]]) = unmkStateGet(p)
  }
  lazy val StateGetRep: Rep[StateGetCompanionAbs] = new StateGetCompanionAbs
  lazy val StateGet: StateGetCompanionAbs = proxyStateGetCompanion(StateGetRep)
  implicit def proxyStateGetCompanion(p: Rep[StateGetCompanionAbs]): StateGetCompanionAbs = {
    proxyOps[StateGetCompanionAbs](p)
  }

  implicit case object StateGetCompanionElem extends CompanionElem[StateGetCompanionAbs] {
    lazy val tag = weakTypeTag[StateGetCompanionAbs]
    protected def getDefaultRep = StateGet
  }

  implicit def proxyStateGet[S, A](p: Rep[StateGet[S, A]]): StateGet[S, A] =
    proxyOps[StateGet[S, A]](p)

  implicit class ExtendedStateGet[S, A](p: Rep[StateGet[S, A]])(implicit eS: Elem[S], eA: Elem[A]) {
    def toData: Rep[StateGetData[S, A]] = isoStateGet(eS, eA).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoStateGet[S, A](implicit eS: Elem[S], eA: Elem[A]): Iso[StateGetData[S, A], StateGet[S, A]] =
    reifyObject(new StateGetIso[S, A]()(eS, eA))

  // 6) smart constructor and deconstructor
  def mkStateGet[S, A](f: Rep[S => A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StateGet[S, A]]
  def unmkStateGet[S, A](p: Rep[StateF[S, A]]): Option[(Rep[S => A])]

  abstract class AbsStatePut[S, A]
      (s: Rep[S], a: Rep[A])(implicit eS: Elem[S], eA: Elem[A])
    extends StatePut[S, A](s, a) with Def[StatePut[S, A]] {
    lazy val selfType = element[StatePut[S, A]]
  }
  // elem for concrete class
  class StatePutElem[S, A](val iso: Iso[StatePutData[S, A], StatePut[S, A]])(implicit override val eS: Elem[S], override val eA: Elem[A])
    extends StateFElem[S, A, StatePut[S, A]]
    with ConcreteElem[StatePutData[S, A], StatePut[S, A]] {
    override lazy val parent: Option[Elem[_]] = Some(stateFElement(element[S], element[A]))
    override lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("S" -> Left(eS), "A" -> Left(eA))
    }

    override def convertStateF(x: Rep[StateF[S, A]]) = // Converter is not generated by meta
!!!("Cannot convert from StateF to StatePut: missing fields List(s, a)")
    override def getDefaultRep = StatePut(element[S].defaultRepValue, element[A].defaultRepValue)
    override lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagA = eA.tag
      weakTypeTag[StatePut[S, A]]
    }
  }

  // state representation type
  type StatePutData[S, A] = (S, A)

  // 3) Iso for concrete class
  class StatePutIso[S, A](implicit eS: Elem[S], eA: Elem[A])
    extends EntityIso[StatePutData[S, A], StatePut[S, A]] with Def[StatePutIso[S, A]] {
    override def from(p: Rep[StatePut[S, A]]) =
      (p.s, p.a)
    override def to(p: Rep[(S, A)]) = {
      val Pair(s, a) = p
      StatePut(s, a)
    }
    lazy val eFrom = pairElement(element[S], element[A])
    lazy val eTo = new StatePutElem[S, A](self)
    lazy val selfType = new StatePutIsoElem[S, A](eS, eA)
    def productArity = 2
    def productElement(n: Int) = (eS, eA).productElement(n)
  }
  case class StatePutIsoElem[S, A](eS: Elem[S], eA: Elem[A]) extends Elem[StatePutIso[S, A]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new StatePutIso[S, A]()(eS, eA))
    lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagA = eA.tag
      weakTypeTag[StatePutIso[S, A]]
    }
  }
  // 4) constructor and deconstructor
  class StatePutCompanionAbs extends CompanionDef[StatePutCompanionAbs] with StatePutCompanion {
    def selfType = StatePutCompanionElem
    override def toString = "StatePut"
    @scalan.OverloadId("fromData")
    def apply[S, A](p: Rep[StatePutData[S, A]])(implicit eS: Elem[S], eA: Elem[A]): Rep[StatePut[S, A]] =
      isoStatePut(eS, eA).to(p)
    @scalan.OverloadId("fromFields")
    def apply[S, A](s: Rep[S], a: Rep[A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StatePut[S, A]] =
      mkStatePut(s, a)

    def unapply[S, A](p: Rep[StateF[S, A]]) = unmkStatePut(p)
  }
  lazy val StatePutRep: Rep[StatePutCompanionAbs] = new StatePutCompanionAbs
  lazy val StatePut: StatePutCompanionAbs = proxyStatePutCompanion(StatePutRep)
  implicit def proxyStatePutCompanion(p: Rep[StatePutCompanionAbs]): StatePutCompanionAbs = {
    proxyOps[StatePutCompanionAbs](p)
  }

  implicit case object StatePutCompanionElem extends CompanionElem[StatePutCompanionAbs] {
    lazy val tag = weakTypeTag[StatePutCompanionAbs]
    protected def getDefaultRep = StatePut
  }

  implicit def proxyStatePut[S, A](p: Rep[StatePut[S, A]]): StatePut[S, A] =
    proxyOps[StatePut[S, A]](p)

  implicit class ExtendedStatePut[S, A](p: Rep[StatePut[S, A]])(implicit eS: Elem[S], eA: Elem[A]) {
    def toData: Rep[StatePutData[S, A]] = isoStatePut(eS, eA).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoStatePut[S, A](implicit eS: Elem[S], eA: Elem[A]): Iso[StatePutData[S, A], StatePut[S, A]] =
    reifyObject(new StatePutIso[S, A]()(eS, eA))

  // 6) smart constructor and deconstructor
  def mkStatePut[S, A](s: Rep[S], a: Rep[A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StatePut[S, A]]
  def unmkStatePut[S, A](p: Rep[StateF[S, A]]): Option[(Rep[S], Rep[A])]

  registerModule(FreeStates_Module)
}

// Std -----------------------------------
trait FreeStatesStd extends scalan.ScalanDslStd with FreeStatesDsl {
  self: MonadsDslStd =>
  lazy val StateF: Rep[StateFCompanionAbs] = new StateFCompanionAbs {
  }

  case class StdStateGet[S, A]
      (override val f: Rep[S => A])(implicit eS: Elem[S], eA: Elem[A])
    extends AbsStateGet[S, A](f) {
  }

  def mkStateGet[S, A]
    (f: Rep[S => A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StateGet[S, A]] =
    new StdStateGet[S, A](f)
  def unmkStateGet[S, A](p: Rep[StateF[S, A]]) = p match {
    case p: StateGet[S, A] @unchecked =>
      Some((p.f))
    case _ => None
  }

  case class StdStatePut[S, A]
      (override val s: Rep[S], override val a: Rep[A])(implicit eS: Elem[S], eA: Elem[A])
    extends AbsStatePut[S, A](s, a) {
  }

  def mkStatePut[S, A]
    (s: Rep[S], a: Rep[A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StatePut[S, A]] =
    new StdStatePut[S, A](s, a)
  def unmkStatePut[S, A](p: Rep[StateF[S, A]]) = p match {
    case p: StatePut[S, A] @unchecked =>
      Some((p.s, p.a))
    case _ => None
  }
}

// Exp -----------------------------------
trait FreeStatesExp extends scalan.ScalanDslExp with FreeStatesDsl {
  self: MonadsDslExp =>
  lazy val StateF: Rep[StateFCompanionAbs] = new StateFCompanionAbs {
  }

  case class ExpStateGet[S, A]
      (override val f: Rep[S => A])(implicit eS: Elem[S], eA: Elem[A])
    extends AbsStateGet[S, A](f)

  object StateGetMethods {
  }

  object StateGetCompanionMethods {
  }

  def mkStateGet[S, A]
    (f: Rep[S => A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StateGet[S, A]] =
    new ExpStateGet[S, A](f)
  def unmkStateGet[S, A](p: Rep[StateF[S, A]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: StateGetElem[S, A] @unchecked =>
      Some((p.asRep[StateGet[S, A]].f))
    case _ =>
      None
  }

  case class ExpStatePut[S, A]
      (override val s: Rep[S], override val a: Rep[A])(implicit eS: Elem[S], eA: Elem[A])
    extends AbsStatePut[S, A](s, a)

  object StatePutMethods {
  }

  object StatePutCompanionMethods {
  }

  def mkStatePut[S, A]
    (s: Rep[S], a: Rep[A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StatePut[S, A]] =
    new ExpStatePut[S, A](s, a)
  def unmkStatePut[S, A](p: Rep[StateF[S, A]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: StatePutElem[S, A] @unchecked =>
      Some((p.asRep[StatePut[S, A]].s, p.asRep[StatePut[S, A]].a))
    case _ =>
      None
  }

  object StateFMethods {
  }

  object StateFCompanionMethods {
    object unit {
      def unapply(d: Def[_]): Option[Rep[A] forSome {type S; type A}] = d match {
        case MethodCall(receiver, method, Seq(a, _*), _) if receiver.elem == StateFCompanionElem && method.getName == "unit" =>
          Some(a).asInstanceOf[Option[Rep[A] forSome {type S; type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[A] forSome {type S; type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object get {
      def unapply(d: Def[_]): Option[Unit forSome {type S}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem == StateFCompanionElem && method.getName == "get" =>
          Some(()).asInstanceOf[Option[Unit forSome {type S}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Unit forSome {type S}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object set {
      def unapply(d: Def[_]): Option[Rep[S] forSome {type S}] = d match {
        case MethodCall(receiver, method, Seq(s, _*), _) if receiver.elem == StateFCompanionElem && method.getName == "set" =>
          Some(s).asInstanceOf[Option[Rep[S] forSome {type S}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[S] forSome {type S}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object run {
      def unapply(d: Def[_]): Option[(Rep[FreeState[S, A]], Rep[S]) forSome {type S; type A}] = d match {
        case MethodCall(receiver, method, Seq(t, s, _*), _) if receiver.elem == StateFCompanionElem && method.getName == "run" =>
          Some((t, s)).asInstanceOf[Option[(Rep[FreeState[S, A]], Rep[S]) forSome {type S; type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[FreeState[S, A]], Rep[S]) forSome {type S; type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }
}

object FreeStates_Module extends scalan.ModuleInfo {
  val dump = "H4sIAAAAAAAAANVWPYwbRRR+a5//j1xC+BFEyR0nAyIidkQTpCsic7GjIOfudJsCmYhovDt2NuzOLjvj05oiBUUK6BANBUUkJJo0KAUNokNCFFQRQqJKQRWCUApSgXgz++O1z2s4kRRsMdqZffN+vu97M3vrPhS4Dy9xg9iENRwqSENX7y0u6nqbCUuML7rmyKbn6ODuF6/fXs9/9XUOVnpQvEr4OW73oBK+tAMvedeF2YUKYQblwvW5gBe6KkLTcG2bGsJyWdNynJEgfZs2uxYXG11Y6rvm+D24DloXDhsuM3wqqL5pE84pj9bLVGZkJfOKmo+3vUkM1pRVNFNVXPKJJTB9jHE4tN+lnj5mLhs7Ag5FqW17Mi20KVmO5/oiDlFCd1ddM54uMYIL8GT3GtkjTQwxbOrCt9gQd9Y8YrxLhnQLTaT5EibMqT24NPbUPN+FKhcmAnTB8Wy1EngAgAy8ppJoTPBpJPg0JD51nfoWsa33ify447vBGMJHywMEHrp49R9cxB5om5n1Dy8bbz/Ua05Obg5kKiVVYREdrWaoQVGBOH63+zF/cP7mmRxUe1C1eKvPhU8MkaY8QqtGGHOFyjkBkPhDZGs9iy0VpYU2M5KoGK7jEYaeIiiXkSfbMiwhjeXacsROBvQl4dHYVAs8Lal3LaNepZtNYts795479eKv7bdykJsOUUGXOgrfj50KKOpYLu1EzuW4IkDTJwjLaUtN5VAJJmNpQS4JKi/f+8389jRcziVYRqH/HX3oosB/+rF255WzOSj3lNg7Nhn2EE7etqmz7W+6TPSg7O5RP/xS2iO2fJtLZ8mkAzKyRQRyGp08oiNgLbMtPSqh21AtoMUA1EIVb7mM1js79T/07z+5JUXqw3L4JezTv6wzf/58aCCUfhHRQYxtHns7geJEFrMe7YyYcefCp0dXjl+5q3gtmq5DLCWuY10o+NjZqpBjEbQHorEa5qq7Dj2y/sB65+ZHQhGmBdNHx3b/Gvbqhtp3YgF38RH25Y0bT//++ZWjqvXKfUs4xKufPkDjxX3yGBsLprW/EjbEZjpIKY2WHJ9NVtWwipQeUfvOUzG1czW1JxXneS2WkDISkKN6nMCSlPXcfgyzyHLQWuRgP/UCynHGykUiwuPZIkTUntntPmXfP/tNDgpvQmGAbcZRfX13xMyYDrwLBQ3EG/GaNk0Hwk984iTwq2cNJmhNC7cz16A1C0hNm674P51r++iCGbQ1Pqd/s8nat50s2L4oqZNqPPVIJbsz+r9JFjNOSzZbJQeSUSrV4lyY83igPTKRZVAzxfLM8hwG5zEfFje52h8bVHL8YGITGVY7PqUqMv41PRGfJS4jJo+K82E944jRo4Magb7+8LOtkz/c/kXddVV55OMNy5K/3MmBEsz0T+WiioU/ramEUWHyElDJ/g0n3yXXRAwAAA=="
}
}
