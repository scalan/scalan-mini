package special.wrappers

import scalan._
import scala.reflect.runtime.universe._
import scala.reflect._
import scala.collection.mutable.WrappedArray

package impl {
// Abs -----------------------------------
trait WrappersSpecDefs extends scalan.Scalan with WrappersSpec {
  self: Library =>
import IsoUR._
import Converter._
import WOption._
import WRType._
import WSpecialPredef._
import WrapSpecBase._
import OptionWrapSpec._
import RTypeWrapSpec._
import SpecialPredefWrapSpec._

object WrapSpecBase extends EntityObject("WrapSpecBase") {
  private val WrapSpecBaseClass = classOf[WrapSpecBase]

  // entityAdapter for WrapSpecBase trait
  case class WrapSpecBaseAdapter(source: Rep[WrapSpecBase])
      extends WrapSpecBase
      with Def[WrapSpecBase] {
    val selfType: Elem[WrapSpecBase] = element[WrapSpecBase]
    override def transform(t: Transformer) = WrapSpecBaseAdapter(t(source))
  }

  // entityProxy: single proxy for each type family
  implicit def proxyWrapSpecBase(p: Rep[WrapSpecBase]): WrapSpecBase = {
    if (p.rhs.isInstanceOf[WrapSpecBase]) p.rhs.asInstanceOf[WrapSpecBase]
    else
      WrapSpecBaseAdapter(p)
  }

  // familyElem
  class WrapSpecBaseElem[To <: WrapSpecBase]
    extends EntityElem[To] {
  }

  implicit lazy val wrapSpecBaseElement: Elem[WrapSpecBase] =
    new WrapSpecBaseElem[WrapSpecBase]

  implicit case object WrapSpecBaseCompanionElem extends CompanionElem[WrapSpecBaseCompanionCtor]

  abstract class WrapSpecBaseCompanionCtor extends CompanionDef[WrapSpecBaseCompanionCtor] with WrapSpecBaseCompanion {
    def selfType = WrapSpecBaseCompanionElem
    override def toString = "WrapSpecBase"
  }
  implicit def proxyWrapSpecBaseCompanionCtor(p: Rep[WrapSpecBaseCompanionCtor]): WrapSpecBaseCompanionCtor =
    p.rhs.asInstanceOf[WrapSpecBaseCompanionCtor]

  lazy val RWrapSpecBase: Rep[WrapSpecBaseCompanionCtor] = new WrapSpecBaseCompanionCtor {
    private val thisClass = classOf[WrapSpecBaseCompanion]
  }

  object WrapSpecBaseMethods {
  }

  object WrapSpecBaseCompanionMethods {
  }
} // of object WrapSpecBase
  registerEntityObject("WrapSpecBase", WrapSpecBase)

object OptionWrapSpec extends EntityObject("OptionWrapSpec") {
  private val OptionWrapSpecClass = classOf[OptionWrapSpec]

  // entityAdapter for OptionWrapSpec trait
  case class OptionWrapSpecAdapter(source: Rep[OptionWrapSpec])
      extends OptionWrapSpec
      with Def[OptionWrapSpec] {
    val selfType: Elem[OptionWrapSpec] = element[OptionWrapSpec]
    override def transform(t: Transformer) = OptionWrapSpecAdapter(t(source))

    override def getOrElse[A](xs: Rep[WOption[A]], default: Rep[Thunk[A]]): Rep[A] = {
      implicit val eA = xs.eA
      asRep[A](mkMethodCall(source,
        OptionWrapSpecClass.getMethod("getOrElse", classOf[Sym], classOf[Sym]),
        Array[AnyRef](xs, default),
        true, true, element[A]))
    }

    override def fold[A, B](xs: Rep[WOption[A]], ifEmpty: Rep[Thunk[B]], f: Rep[A => B]): Rep[B] = {
      implicit val eA = xs.eA
implicit val eB = ifEmpty.elem.eItem
      asRep[B](mkMethodCall(source,
        OptionWrapSpecClass.getMethod("fold", classOf[Sym], classOf[Sym], classOf[Sym]),
        Array[AnyRef](xs, ifEmpty, f),
        true, true, element[B]))
    }
  }

  // entityProxy: single proxy for each type family
  implicit def proxyOptionWrapSpec(p: Rep[OptionWrapSpec]): OptionWrapSpec = {
    if (p.rhs.isInstanceOf[OptionWrapSpec]) p.rhs.asInstanceOf[OptionWrapSpec]
    else
      OptionWrapSpecAdapter(p)
  }

  // familyElem
  class OptionWrapSpecElem[To <: OptionWrapSpec]
    extends WrapSpecBaseElem[To] {
    override lazy val parent: Option[Elem[_]] = Some(wrapSpecBaseElement)
  }

  implicit lazy val optionWrapSpecElement: Elem[OptionWrapSpec] =
    new OptionWrapSpecElem[OptionWrapSpec]

  implicit case object OptionWrapSpecCompanionElem extends CompanionElem[OptionWrapSpecCompanionCtor]

  abstract class OptionWrapSpecCompanionCtor extends CompanionDef[OptionWrapSpecCompanionCtor] with OptionWrapSpecCompanion {
    def selfType = OptionWrapSpecCompanionElem
    override def toString = "OptionWrapSpec"
  }
  implicit def proxyOptionWrapSpecCompanionCtor(p: Rep[OptionWrapSpecCompanionCtor]): OptionWrapSpecCompanionCtor =
    p.rhs.asInstanceOf[OptionWrapSpecCompanionCtor]

  lazy val ROptionWrapSpec: Rep[OptionWrapSpecCompanionCtor] = new OptionWrapSpecCompanionCtor {
    private val thisClass = classOf[OptionWrapSpecCompanion]
  }

  object OptionWrapSpecMethods {
    object get {
      def unapply(d: Def[_]): Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]]) forSome {type A}] = d match {
        case MethodCall(receiver, method, args, _) if method.getName == "get" && receiver.elem.isInstanceOf[OptionWrapSpecElem[_]] =>
          val res = (receiver, args(0))
          Nullable(res).asInstanceOf[Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]]) forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]]) forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object getOrElse {
      def unapply(d: Def[_]): Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]], Rep[Thunk[A]]) forSome {type A}] = d match {
        case MethodCall(receiver, method, args, _) if method.getName == "getOrElse" && receiver.elem.isInstanceOf[OptionWrapSpecElem[_]] =>
          val res = (receiver, args(0), args(1))
          Nullable(res).asInstanceOf[Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]], Rep[Thunk[A]]) forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]], Rep[Thunk[A]]) forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object map {
      def unapply(d: Def[_]): Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]], Rep[A => B]) forSome {type A; type B}] = d match {
        case MethodCall(receiver, method, args, _) if method.getName == "map" && receiver.elem.isInstanceOf[OptionWrapSpecElem[_]] =>
          val res = (receiver, args(0), args(1))
          Nullable(res).asInstanceOf[Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]], Rep[A => B]) forSome {type A; type B}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]], Rep[A => B]) forSome {type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object flatMap {
      def unapply(d: Def[_]): Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]], Rep[A => WOption[B]]) forSome {type A; type B}] = d match {
        case MethodCall(receiver, method, args, _) if method.getName == "flatMap" && receiver.elem.isInstanceOf[OptionWrapSpecElem[_]] =>
          val res = (receiver, args(0), args(1))
          Nullable(res).asInstanceOf[Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]], Rep[A => WOption[B]]) forSome {type A; type B}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]], Rep[A => WOption[B]]) forSome {type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object filter {
      def unapply(d: Def[_]): Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]], Rep[A => Boolean]) forSome {type A}] = d match {
        case MethodCall(receiver, method, args, _) if method.getName == "filter" && receiver.elem.isInstanceOf[OptionWrapSpecElem[_]] =>
          val res = (receiver, args(0), args(1))
          Nullable(res).asInstanceOf[Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]], Rep[A => Boolean]) forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]], Rep[A => Boolean]) forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object isDefined {
      def unapply(d: Def[_]): Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]]) forSome {type A}] = d match {
        case MethodCall(receiver, method, args, _) if method.getName == "isDefined" && receiver.elem.isInstanceOf[OptionWrapSpecElem[_]] =>
          val res = (receiver, args(0))
          Nullable(res).asInstanceOf[Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]]) forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]]) forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object isEmpty {
      def unapply(d: Def[_]): Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]]) forSome {type A}] = d match {
        case MethodCall(receiver, method, args, _) if method.getName == "isEmpty" && receiver.elem.isInstanceOf[OptionWrapSpecElem[_]] =>
          val res = (receiver, args(0))
          Nullable(res).asInstanceOf[Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]]) forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]]) forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object fold {
      def unapply(d: Def[_]): Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]], Rep[Thunk[B]], Rep[A => B]) forSome {type A; type B}] = d match {
        case MethodCall(receiver, method, args, _) if method.getName == "fold" && receiver.elem.isInstanceOf[OptionWrapSpecElem[_]] =>
          val res = (receiver, args(0), args(1), args(2))
          Nullable(res).asInstanceOf[Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]], Rep[Thunk[B]], Rep[A => B]) forSome {type A; type B}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[OptionWrapSpec], Rep[WOption[A]], Rep[Thunk[B]], Rep[A => B]) forSome {type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }
  }

  object OptionWrapSpecCompanionMethods {
  }
} // of object OptionWrapSpec
  registerEntityObject("OptionWrapSpec", OptionWrapSpec)

object SpecialPredefWrapSpec extends EntityObject("SpecialPredefWrapSpec") {
  private val SpecialPredefWrapSpecClass = classOf[SpecialPredefWrapSpec]

  // entityAdapter for SpecialPredefWrapSpec trait
  case class SpecialPredefWrapSpecAdapter(source: Rep[SpecialPredefWrapSpec])
      extends SpecialPredefWrapSpec
      with Def[SpecialPredefWrapSpec] {
    val selfType: Elem[SpecialPredefWrapSpec] = element[SpecialPredefWrapSpec]
    override def transform(t: Transformer) = SpecialPredefWrapSpecAdapter(t(source))
  }

  // entityProxy: single proxy for each type family
  implicit def proxySpecialPredefWrapSpec(p: Rep[SpecialPredefWrapSpec]): SpecialPredefWrapSpec = {
    if (p.rhs.isInstanceOf[SpecialPredefWrapSpec]) p.rhs.asInstanceOf[SpecialPredefWrapSpec]
    else
      SpecialPredefWrapSpecAdapter(p)
  }

  // familyElem
  class SpecialPredefWrapSpecElem[To <: SpecialPredefWrapSpec]
    extends WrapSpecBaseElem[To] {
    override lazy val parent: Option[Elem[_]] = Some(wrapSpecBaseElement)
  }

  implicit lazy val specialPredefWrapSpecElement: Elem[SpecialPredefWrapSpec] =
    new SpecialPredefWrapSpecElem[SpecialPredefWrapSpec]

  implicit case object SpecialPredefWrapSpecCompanionElem extends CompanionElem[SpecialPredefWrapSpecCompanionCtor]

  abstract class SpecialPredefWrapSpecCompanionCtor extends CompanionDef[SpecialPredefWrapSpecCompanionCtor] with SpecialPredefWrapSpecCompanion {
    def selfType = SpecialPredefWrapSpecCompanionElem
    override def toString = "SpecialPredefWrapSpec"
  }
  implicit def proxySpecialPredefWrapSpecCompanionCtor(p: Rep[SpecialPredefWrapSpecCompanionCtor]): SpecialPredefWrapSpecCompanionCtor =
    p.rhs.asInstanceOf[SpecialPredefWrapSpecCompanionCtor]

  lazy val RSpecialPredefWrapSpec: Rep[SpecialPredefWrapSpecCompanionCtor] = new SpecialPredefWrapSpecCompanionCtor {
    private val thisClass = classOf[SpecialPredefWrapSpecCompanion]
  }

  object SpecialPredefWrapSpecMethods {
    object loopUntil {
      def unapply(d: Def[_]): Nullable[(Rep[SpecialPredefWrapSpec], Rep[A], Rep[A => Boolean], Rep[A => A]) forSome {type A}] = d match {
        case MethodCall(receiver, method, args, _) if method.getName == "loopUntil" && receiver.elem.isInstanceOf[SpecialPredefWrapSpecElem[_]] =>
          val res = (receiver, args(0), args(1), args(2))
          Nullable(res).asInstanceOf[Nullable[(Rep[SpecialPredefWrapSpec], Rep[A], Rep[A => Boolean], Rep[A => A]) forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[SpecialPredefWrapSpec], Rep[A], Rep[A => Boolean], Rep[A => A]) forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object cast {
      def unapply(d: Def[_]): Nullable[(Rep[SpecialPredefWrapSpec], Rep[Any], Elem[A]) forSome {type A}] = d match {
        case MethodCall(receiver, method, args, _) if method.getName == "cast" && receiver.elem.isInstanceOf[SpecialPredefWrapSpecElem[_]] =>
          val res = (receiver, args(0), args(1))
          Nullable(res).asInstanceOf[Nullable[(Rep[SpecialPredefWrapSpec], Rep[Any], Elem[A]) forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[SpecialPredefWrapSpec], Rep[Any], Elem[A]) forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object some {
      def unapply(d: Def[_]): Nullable[(Rep[SpecialPredefWrapSpec], Rep[A]) forSome {type A}] = d match {
        case MethodCall(receiver, method, args, _) if method.getName == "some" && receiver.elem.isInstanceOf[SpecialPredefWrapSpecElem[_]] =>
          val res = (receiver, args(0))
          Nullable(res).asInstanceOf[Nullable[(Rep[SpecialPredefWrapSpec], Rep[A]) forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[SpecialPredefWrapSpec], Rep[A]) forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object none {
      def unapply(d: Def[_]): Nullable[(Rep[SpecialPredefWrapSpec], Elem[A]) forSome {type A}] = d match {
        case MethodCall(receiver, method, args, _) if method.getName == "none" && receiver.elem.isInstanceOf[SpecialPredefWrapSpecElem[_]] =>
          val res = (receiver, args(0))
          Nullable(res).asInstanceOf[Nullable[(Rep[SpecialPredefWrapSpec], Elem[A]) forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[SpecialPredefWrapSpec], Elem[A]) forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object optionGetOrElse {
      def unapply(d: Def[_]): Nullable[(Rep[SpecialPredefWrapSpec], Rep[WOption[A]], Rep[A]) forSome {type A}] = d match {
        case MethodCall(receiver, method, args, _) if method.getName == "optionGetOrElse" && receiver.elem.isInstanceOf[SpecialPredefWrapSpecElem[_]] =>
          val res = (receiver, args(0), args(1))
          Nullable(res).asInstanceOf[Nullable[(Rep[SpecialPredefWrapSpec], Rep[WOption[A]], Rep[A]) forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[SpecialPredefWrapSpec], Rep[WOption[A]], Rep[A]) forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }
  }

  object SpecialPredefWrapSpecCompanionMethods {
  }
} // of object SpecialPredefWrapSpec
  registerEntityObject("SpecialPredefWrapSpec", SpecialPredefWrapSpec)

object RTypeWrapSpec extends EntityObject("RTypeWrapSpec") {
  private val RTypeWrapSpecClass = classOf[RTypeWrapSpec]

  // entityAdapter for RTypeWrapSpec trait
  case class RTypeWrapSpecAdapter(source: Rep[RTypeWrapSpec])
      extends RTypeWrapSpec
      with Def[RTypeWrapSpec] {
    val selfType: Elem[RTypeWrapSpec] = element[RTypeWrapSpec]
    override def transform(t: Transformer) = RTypeWrapSpecAdapter(t(source))
  }

  // entityProxy: single proxy for each type family
  implicit def proxyRTypeWrapSpec(p: Rep[RTypeWrapSpec]): RTypeWrapSpec = {
    if (p.rhs.isInstanceOf[RTypeWrapSpec]) p.rhs.asInstanceOf[RTypeWrapSpec]
    else
      RTypeWrapSpecAdapter(p)
  }

  // familyElem
  class RTypeWrapSpecElem[To <: RTypeWrapSpec]
    extends WrapSpecBaseElem[To] {
    override lazy val parent: Option[Elem[_]] = Some(wrapSpecBaseElement)
  }

  implicit lazy val rTypeWrapSpecElement: Elem[RTypeWrapSpec] =
    new RTypeWrapSpecElem[RTypeWrapSpec]

  implicit case object RTypeWrapSpecCompanionElem extends CompanionElem[RTypeWrapSpecCompanionCtor]

  abstract class RTypeWrapSpecCompanionCtor extends CompanionDef[RTypeWrapSpecCompanionCtor] with RTypeWrapSpecCompanion {
    def selfType = RTypeWrapSpecCompanionElem
    override def toString = "RTypeWrapSpec"
  }
  implicit def proxyRTypeWrapSpecCompanionCtor(p: Rep[RTypeWrapSpecCompanionCtor]): RTypeWrapSpecCompanionCtor =
    p.rhs.asInstanceOf[RTypeWrapSpecCompanionCtor]

  lazy val RRTypeWrapSpec: Rep[RTypeWrapSpecCompanionCtor] = new RTypeWrapSpecCompanionCtor {
    private val thisClass = classOf[RTypeWrapSpecCompanion]
  }

  object RTypeWrapSpecMethods {
    object name {
      def unapply(d: Def[_]): Nullable[(Rep[RTypeWrapSpec], Rep[WRType[T]]) forSome {type T}] = d match {
        case MethodCall(receiver, method, args, _) if method.getName == "name" && receiver.elem.isInstanceOf[RTypeWrapSpecElem[_]] =>
          val res = (receiver, args(0))
          Nullable(res).asInstanceOf[Nullable[(Rep[RTypeWrapSpec], Rep[WRType[T]]) forSome {type T}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[RTypeWrapSpec], Rep[WRType[T]]) forSome {type T}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }
  }

  object RTypeWrapSpecCompanionMethods {
  }
} // of object RTypeWrapSpec
  registerEntityObject("RTypeWrapSpec", RTypeWrapSpec)

  registerModule(WrappersSpecModule)
}

object WrappersSpecModule extends scalan.ModuleInfo("special.wrappers", "WrappersSpec")
}

trait WrappersSpecModule extends special.wrappers.impl.WrappersSpecDefs {self: Library =>}
