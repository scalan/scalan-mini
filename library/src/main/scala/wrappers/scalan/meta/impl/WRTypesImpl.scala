package wrappers.scalan.meta

import scalan._
import impl._
import special.wrappers.WrappersModule
import scala.reflect.runtime.universe._
import scala.reflect._

package impl {
  import special.wrappers.RTypeWrapSpec

  import scalan.meta.RType

  // Abs -----------------------------------
trait WRTypesDefs extends scalan.Scalan with WRTypes {
  self: WrappersModule =>
import IsoUR._
import Converter._
import WRType._

object WRType extends EntityObject("WRType") {
  // entityConst: single const for each entity
  import Liftables._
  import scala.reflect.{ClassTag, classTag}

  case class WRTypeConst[SA, A](
        constValue: RType[SA],
        lA: Liftable[SA, A]
      ) extends WRType[A] with LiftedConst[RType[SA], WRType[A]] {
    implicit def eA: Elem[A] = lA.eW
    val liftable: Liftable[RType[SA], WRType[A]] = liftableRType(lA)
    val selfType: Elem[WRType[A]] = liftable.eW
    @External def name: Rep[String] = delayInvoke
  }

  case class LiftableRType[SA, A](lA: Liftable[SA, A])
    extends Liftable[RType[SA], WRType[A]] {
    lazy val eW: Elem[WRType[A]] = wRTypeElement(lA.eW)
    lazy val sourceClassTag: ClassTag[RType[SA]] = {
      implicit val tagSA = lA.eW.sourceClassTag.asInstanceOf[ClassTag[SA]]
      classTag[RType[SA]]
    }
    def lift(x: RType[SA]): Rep[WRType[A]] = WRTypeConst(x, lA)
    def unlift(w: Rep[WRType[A]]): RType[SA] = w match {
      case Def(WRTypeConst(x: RType[_], _lA))
            if _lA == lA => x.asInstanceOf[RType[SA]]
      case _ => unliftError(w)
    }
  }
  implicit def liftableRType[SA, A](implicit lA: Liftable[SA,A]): Liftable[RType[SA], WRType[A]] =
    LiftableRType(lA)

  private val _RTypeWrapSpec = new RTypeWrapSpec
  // entityProxy: single proxy for each type family
  implicit def proxyWRType[A](p: Rep[WRType[A]]): WRType[A] = {
    proxyOps[WRType[A]](p)(scala.reflect.classTag[WRType[A]])
  }

  // familyElem
  class WRTypeElem[A, To <: WRType[A]](implicit _eA: Elem[A])
    extends EntityElem[To] {
    def eA = _eA

    override val liftable = liftableRType(_eA.liftable).asLiftable[RType[_], To]

    override protected def collectMethods: Map[java.lang.reflect.Method, MethodDesc] = {
      super.collectMethods ++
        Elem.declaredWrapperMethods(_RTypeWrapSpec, classOf[WRType[A]], Set(
        "name"
        ))
    }

    lazy val parent: Option[Elem[_]] = None
    override def buildTypeArgs = super.buildTypeArgs ++ TypeArgs("A" -> (eA -> scalan.util.Invariant))
    override lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[WRType[A]].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      val conv = fun {x: Rep[WRType[A]] => convertWRType(x) }
      tryConvert(element[WRType[A]], this, x, conv)
    }

    def convertWRType(x: Rep[WRType[A]]): Rep[To] = {
      x.elem match {
        case _: WRTypeElem[_, _] => x.asRep[To]
        case e => !!!(s"Expected $x to have WRTypeElem[_, _], but got $e", x)
      }
    }
    override def getDefaultRep: Rep[To] = ???
  }

  implicit def wRTypeElement[A](implicit eA: Elem[A]): Elem[WRType[A]] =
    cachedElem[WRTypeElem[A, WRType[A]]](eA)

  implicit case object WRTypeCompanionElem extends CompanionElem[WRTypeCompanionCtor] {
    lazy val tag = weakTypeTag[WRTypeCompanionCtor]
    protected def getDefaultRep = RWRType
  }

  abstract class WRTypeCompanionCtor extends CompanionDef[WRTypeCompanionCtor] with WRTypeCompanion {
    def selfType = WRTypeCompanionElem
    override def toString = "WRType"
  }
  implicit def proxyWRTypeCompanionCtor(p: Rep[WRTypeCompanionCtor]): WRTypeCompanionCtor =
    proxyOps[WRTypeCompanionCtor](p)

  lazy val RWRType: Rep[WRTypeCompanionCtor] = new WRTypeCompanionCtor {
  }

  object WRTypeMethods {
    object name {
      def unapply(d: Def[_]): Option[Rep[WRType[A]] forSome {type A}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[WRTypeElem[_, _]] && method.getName == "name" =>
          Some(receiver).asInstanceOf[Option[Rep[WRType[A]] forSome {type A}]]
        case _ => None
      }
      def unapply(exp: Sym): Option[Rep[WRType[A]] forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object WRTypeCompanionMethods {
  }
} // of object WRType
  registerEntityObject("WRType", WRType)

  registerModule(WRTypesModule)
}

object WRTypesModule extends scalan.ModuleInfo("wrappers.scalan.meta", "WRTypes")
}

trait WRTypesModule extends wrappers.scalan.meta.impl.WRTypesDefs {self: WrappersModule =>}