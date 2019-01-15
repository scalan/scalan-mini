package special.collection

import scalan._
import scala.reflect.runtime.universe._
import scala.reflect._

package impl {
// Abs -----------------------------------
trait CollsDefs extends scalan.Scalan with Colls {
  self: Library =>
import IsoUR._
import Converter._
import ColBuilder._
import Coll._
import Monoid._
import PairColl._
import WArray._
import ReplColl._

object Coll extends EntityObject("Coll") {
  // entityConst: single const for each entity
  import Liftables._
  import scala.reflect.{ClassTag, classTag}
  type SColl[A] = special.collection.Coll[A]
  case class CollConst[SA, A](
        constValue: SColl[SA],
        lA: Liftable[SA, A]
      ) extends Coll[A] with LiftedConst[SColl[SA], Coll[A]]
        with Def[Coll[A]] with CollConstMethods[A] {
    implicit def eA: Elem[A] = lA.eW

    val liftable: Liftable[SColl[SA], Coll[A]] = liftableColl(lA)
    val selfType: Elem[Coll[A]] = liftable.eW
  }

  trait CollConstMethods[A] extends Coll[A]  { thisConst: Def[_] =>
    implicit def eA: Elem[A]
    private val CollClass = classOf[Coll[A]]

    override def builder: Rep[ColBuilder] = {
      asRep[ColBuilder](mkMethodCall(self,
        CollClass.getMethod("builder"),
        List(),
        true, false, element[ColBuilder]))
    }

    override def arr: Rep[WArray[A]] = {
      asRep[WArray[A]](mkMethodCall(self,
        CollClass.getMethod("arr"),
        List(),
        true, false, element[WArray[A]]))
    }

    override def length: Rep[Int] = {
      asRep[Int](mkMethodCall(self,
        CollClass.getMethod("length"),
        List(),
        true, false, element[Int]))
    }

    override def apply(i: Rep[Int]): Rep[A] = {
      asRep[A](mkMethodCall(self,
        CollClass.getMethod("apply", classOf[Sym]),
        List(i),
        true, false, element[A]))
    }

    override def getOrElse(i: Rep[Int], default: Rep[A]): Rep[A] = {
      asRep[A](mkMethodCall(self,
        CollClass.getMethod("getOrElse", classOf[Sym], classOf[Sym]),
        List(i, default),
        true, false, element[A]))
    }

    override def map[B](f: Rep[A => B]): Rep[Coll[B]] = {
      implicit val eB = f.elem.eRange
      asRep[Coll[B]](mkMethodCall(self,
        CollClass.getMethod("map", classOf[Sym]),
        List(f),
        true, false, element[Coll[B]]))
    }

    override def zip[B](ys: Rep[Coll[B]]): Rep[PairColl[A, B]] = {
      implicit val eB = ys.eA
      asRep[PairColl[A, B]](mkMethodCall(self,
        CollClass.getMethod("zip", classOf[Sym]),
        List(ys),
        true, false, element[PairColl[A, B]]))
    }

    override def foreach(f: Rep[A => Unit]): Rep[Unit] = {
      asRep[Unit](mkMethodCall(self,
        CollClass.getMethod("foreach", classOf[Sym]),
        List(f),
        true, false, element[Unit]))
    }

    override def exists(p: Rep[A => Boolean]): Rep[Boolean] = {
      asRep[Boolean](mkMethodCall(self,
        CollClass.getMethod("exists", classOf[Sym]),
        List(p),
        true, false, element[Boolean]))
    }

    override def forall(p: Rep[A => Boolean]): Rep[Boolean] = {
      asRep[Boolean](mkMethodCall(self,
        CollClass.getMethod("forall", classOf[Sym]),
        List(p),
        true, false, element[Boolean]))
    }

    override def filter(p: Rep[A => Boolean]): Rep[Coll[A]] = {
      asRep[Coll[A]](mkMethodCall(self,
        CollClass.getMethod("filter", classOf[Sym]),
        List(p),
        true, false, element[Coll[A]]))
    }

    override def fold[B](zero: Rep[B], op: Rep[((B, A)) => B]): Rep[B] = {
      implicit val eB = zero.elem
      asRep[B](mkMethodCall(self,
        CollClass.getMethod("fold", classOf[Sym], classOf[Sym]),
        List(zero, op),
        true, false, element[B]))
    }

    override def sum(m: Rep[Monoid[A]]): Rep[A] = {
      asRep[A](mkMethodCall(self,
        CollClass.getMethod("sum", classOf[Sym]),
        List(m),
        true, false, element[A]))
    }

    override def slice(from: Rep[Int], until: Rep[Int]): Rep[Coll[A]] = {
      asRep[Coll[A]](mkMethodCall(self,
        CollClass.getMethod("slice", classOf[Sym], classOf[Sym]),
        List(from, until),
        true, false, element[Coll[A]]))
    }

    override def append(other: Rep[Coll[A]]): Rep[Coll[A]] = {
      asRep[Coll[A]](mkMethodCall(self,
        CollClass.getMethod("append", classOf[Sym]),
        List(other),
        true, false, element[Coll[A]]))
    }
  }

  case class LiftableColl[SA, A](lA: Liftable[SA, A])
    extends Liftable[SColl[SA], Coll[A]] {
    lazy val eW: Elem[Coll[A]] = collElement(lA.eW)
    lazy val sourceClassTag: ClassTag[SColl[SA]] = {
            implicit val tagSA = lA.eW.sourceClassTag.asInstanceOf[ClassTag[SA]]
      classTag[SColl[SA]]
    }
    def lift(x: SColl[SA]): Rep[Coll[A]] = CollConst(x, lA)
    def unlift(w: Rep[Coll[A]]): SColl[SA] = w match {
      case Def(CollConst(x: SColl[_], _lA))
            if _lA == lA => x.asInstanceOf[SColl[SA]]
      case _ => unliftError(w)
    }
  }
  implicit def liftableColl[SA, A](implicit lA: Liftable[SA,A]): Liftable[SColl[SA], Coll[A]] =
    LiftableColl(lA)

  // entityAdapter for Coll trait
  case class CollAdapter[A](source: Rep[Coll[A]])
      extends Coll[A] with Def[Coll[A]] {
    implicit lazy val eA = source.elem.typeArgs("A")._1.asElem[A]

    val selfType: Elem[Coll[A]] = element[Coll[A]]
    override def transform(t: Transformer) = CollAdapter[A](t(source))
    private val thisClass = classOf[Coll[A]]

    def builder: Rep[ColBuilder] = {
      asRep[ColBuilder](mkMethodCall(source,
        thisClass.getMethod("builder"),
        List(),
        true, true, element[ColBuilder]))
    }

    def arr: Rep[WArray[A]] = {
      asRep[WArray[A]](mkMethodCall(source,
        thisClass.getMethod("arr"),
        List(),
        true, true, element[WArray[A]]))
    }

    def length: Rep[Int] = {
      asRep[Int](mkMethodCall(source,
        thisClass.getMethod("length"),
        List(),
        true, true, element[Int]))
    }

    def apply(i: Rep[Int]): Rep[A] = {
      asRep[A](mkMethodCall(source,
        thisClass.getMethod("apply", classOf[Sym]),
        List(i),
        true, true, element[A]))
    }

    def getOrElse(i: Rep[Int], default: Rep[A]): Rep[A] = {
      asRep[A](mkMethodCall(source,
        thisClass.getMethod("getOrElse", classOf[Sym], classOf[Sym]),
        List(i, default),
        true, true, element[A]))
    }

    def map[B](f: Rep[A => B]): Rep[Coll[B]] = {
      implicit val eB = f.elem.eRange
      asRep[Coll[B]](mkMethodCall(source,
        thisClass.getMethod("map", classOf[Sym]),
        List(f),
        true, true, element[Coll[B]]))
    }

    def zip[B](ys: Rep[Coll[B]]): Rep[PairColl[A, B]] = {
      implicit val eB = ys.eA
      asRep[PairColl[A, B]](mkMethodCall(source,
        thisClass.getMethod("zip", classOf[Sym]),
        List(ys),
        true, true, element[PairColl[A, B]]))
    }

    def foreach(f: Rep[A => Unit]): Rep[Unit] = {
      asRep[Unit](mkMethodCall(source,
        thisClass.getMethod("foreach", classOf[Sym]),
        List(f),
        true, true, element[Unit]))
    }

    def exists(p: Rep[A => Boolean]): Rep[Boolean] = {
      asRep[Boolean](mkMethodCall(source,
        thisClass.getMethod("exists", classOf[Sym]),
        List(p),
        true, true, element[Boolean]))
    }

    def forall(p: Rep[A => Boolean]): Rep[Boolean] = {
      asRep[Boolean](mkMethodCall(source,
        thisClass.getMethod("forall", classOf[Sym]),
        List(p),
        true, true, element[Boolean]))
    }

    def filter(p: Rep[A => Boolean]): Rep[Coll[A]] = {
      asRep[Coll[A]](mkMethodCall(source,
        thisClass.getMethod("filter", classOf[Sym]),
        List(p),
        true, true, element[Coll[A]]))
    }

    def fold[B](zero: Rep[B], op: Rep[((B, A)) => B]): Rep[B] = {
      implicit val eB = zero.elem
      asRep[B](mkMethodCall(source,
        thisClass.getMethod("fold", classOf[Sym], classOf[Sym]),
        List(zero, op),
        true, true, element[B]))
    }

    def sum(m: Rep[Monoid[A]]): Rep[A] = {
      asRep[A](mkMethodCall(source,
        thisClass.getMethod("sum", classOf[Sym]),
        List(m),
        true, true, element[A]))
    }

    def slice(from: Rep[Int], until: Rep[Int]): Rep[Coll[A]] = {
      asRep[Coll[A]](mkMethodCall(source,
        thisClass.getMethod("slice", classOf[Sym], classOf[Sym]),
        List(from, until),
        true, true, element[Coll[A]]))
    }

    def append(other: Rep[Coll[A]]): Rep[Coll[A]] = {
      asRep[Coll[A]](mkMethodCall(source,
        thisClass.getMethod("append", classOf[Sym]),
        List(other),
        true, true, element[Coll[A]]))
    }
  }

  // entityProxy: single proxy for each type family
  implicit def proxyColl[A](p: Rep[Coll[A]]): Coll[A] = {
    if (p.rhs.isInstanceOf[Coll[A]@unchecked]) p.rhs.asInstanceOf[Coll[A]]
    else
      CollAdapter(p)
  }

  implicit def castCollElement[A](elem: Elem[Coll[A]]): CollElem[A, Coll[A]] =
    elem.asInstanceOf[CollElem[A, Coll[A]]]

  implicit lazy val containerColl: Functor[Coll] = new Functor[Coll] {
    def tag[A](implicit evA: WeakTypeTag[A]) = weakTypeTag[Coll[A]]
    def lift[A](implicit evA: Elem[A]) = element[Coll[A]]
    def unlift[A](implicit eFT: Elem[Coll[A]]) =
      castCollElement(eFT).eA
    def getElem[A](fa: Rep[Coll[A]]) = fa.elem
    def unapply[T](e: Elem[_]) = e match {
      case e: CollElem[_,_] => Some(e.asElem[Coll[T]])
      case _ => None
    }
    def map[A,B](xs: Rep[Coll[A]])(f: Rep[A] => Rep[B]) = { implicit val eA = unlift(xs.elem); xs.map(fun(f))}
  }

  case class CollIso[A, B](innerIso: Iso[A, B]) extends Iso1UR[A, B, Coll] {
    lazy val selfType = new ConcreteIsoElem[Coll[A], Coll[B], CollIso[A, B]](eFrom, eTo).
      asInstanceOf[Elem[IsoUR[Coll[A], Coll[B]]]]
    def cC = container[Coll]
    def from(x: Rep[Coll[B]]) = x.map(innerIso.fromFun)
    def to(x: Rep[Coll[A]]) = x.map(innerIso.toFun)
    override def transform(t: Transformer) = CollIso(t(innerIso))
  }

  def collIso[A, B](innerIso: Iso[A, B]) =
    reifyObject(CollIso[A, B](innerIso)).asInstanceOf[Iso1[A, B, Coll]]

  // familyElem
  class CollElem[A, To <: Coll[A]](implicit _eA: Elem[A])
    extends EntityElem1[A, To, Coll](_eA, container[Coll]) {
    def eA = _eA

    override val liftable: Liftables.Liftable[_, To] = liftableColl(_eA.liftable).asLiftable[SColl[_], To]

    override protected def collectMethods: Map[java.lang.reflect.Method, MethodDesc] = {
      super.collectMethods ++
        Elem.declaredMethods(classOf[Coll[A]], classOf[SColl[_]], Set(
        "builder", "arr", "length", "apply", "getOrElse", "map", "zip", "foreach", "exists", "forall", "filter", "where", "fold", "sum", "slice", "append"
        ))
    }

    lazy val parent: Option[Elem[_]] = None
    override def buildTypeArgs = super.buildTypeArgs ++ TypeArgs("A" -> (eA -> scalan.util.Invariant))
    override lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[Coll[A]].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      val conv = fun {x: Rep[Coll[A]] => convertColl(x) }
      tryConvert(element[Coll[A]], this, x, conv)
    }

    def convertColl(x: Rep[Coll[A]]): Rep[To] = {
      x.elem match {
        case _: CollElem[_, _] => asRep[To](x)
        case e => !!!(s"Expected $x to have CollElem[_, _], but got $e", x)
      }
    }
    override def getDefaultRep: Rep[To] = ???
  }

  implicit def collElement[A](implicit eA: Elem[A]): Elem[Coll[A]] =
    cachedElem[CollElem[A, Coll[A]]](eA)

  implicit case object CollCompanionElem extends CompanionElem[CollCompanionCtor] {
    lazy val tag = weakTypeTag[CollCompanionCtor]
    protected def getDefaultRep = RColl
  }

  abstract class CollCompanionCtor extends CompanionDef[CollCompanionCtor] with CollCompanion {
    def selfType = CollCompanionElem
    override def toString = "Coll"
  }
  implicit def proxyCollCompanionCtor(p: Rep[CollCompanionCtor]): CollCompanionCtor =
    proxyOps[CollCompanionCtor](p)

  lazy val RColl: Rep[CollCompanionCtor] = new CollCompanionCtor {
    private val thisClass = classOf[CollCompanion]
  }

  case class ViewColl[A, B](source: Rep[Coll[A]], override val innerIso: Iso[A, B])
    extends View1[A, B, Coll](collIso(innerIso)) {
    override def transform(t: Transformer) = ViewColl(t(source), t(innerIso))
    override def toString = s"ViewColl[${innerIso.eTo.name}]($source)"
    override def equals(other: Any) = other match {
      case v: ViewColl[_, _] => source == v.source && innerIso.eTo == v.innerIso.eTo
      case _ => false
    }
  }

  object CollMethods {
    object builder {
      def unapply(d: Def[_]): Nullable[Rep[Coll[A]] forSome {type A}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[CollElem[_, _]] && method.getName == "builder" =>
          val res = receiver
          Nullable(res).asInstanceOf[Nullable[Rep[Coll[A]] forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[Rep[Coll[A]] forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object arr {
      def unapply(d: Def[_]): Nullable[Rep[Coll[A]] forSome {type A}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[CollElem[_, _]] && method.getName == "arr" =>
          val res = receiver
          Nullable(res).asInstanceOf[Nullable[Rep[Coll[A]] forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[Rep[Coll[A]] forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object length {
      def unapply(d: Def[_]): Nullable[Rep[Coll[A]] forSome {type A}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[CollElem[_, _]] && method.getName == "length" =>
          val res = receiver
          Nullable(res).asInstanceOf[Nullable[Rep[Coll[A]] forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[Rep[Coll[A]] forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object apply {
      def unapply(d: Def[_]): Nullable[(Rep[Coll[A]], Rep[Int]) forSome {type A}] = d match {
        case MethodCall(receiver, method, args, _) if receiver.elem.isInstanceOf[CollElem[_, _]] && method.getName == "apply" =>
          val res = (receiver, args(0))
          Nullable(res).asInstanceOf[Nullable[(Rep[Coll[A]], Rep[Int]) forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[Coll[A]], Rep[Int]) forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object getOrElse {
      def unapply(d: Def[_]): Nullable[(Rep[Coll[A]], Rep[Int], Rep[A]) forSome {type A}] = d match {
        case MethodCall(receiver, method, args, _) if receiver.elem.isInstanceOf[CollElem[_, _]] && method.getName == "getOrElse" =>
          val res = (receiver, args(0), args(1))
          Nullable(res).asInstanceOf[Nullable[(Rep[Coll[A]], Rep[Int], Rep[A]) forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[Coll[A]], Rep[Int], Rep[A]) forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object map {
      def unapply(d: Def[_]): Nullable[(Rep[Coll[A]], Rep[A => B]) forSome {type A; type B}] = d match {
        case MethodCall(receiver, method, args, _) if receiver.elem.isInstanceOf[CollElem[_, _]] && method.getName == "map" =>
          val res = (receiver, args(0))
          Nullable(res).asInstanceOf[Nullable[(Rep[Coll[A]], Rep[A => B]) forSome {type A; type B}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[Coll[A]], Rep[A => B]) forSome {type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object zip {
      def unapply(d: Def[_]): Nullable[(Rep[Coll[A]], Rep[Coll[B]]) forSome {type A; type B}] = d match {
        case MethodCall(receiver, method, args, _) if receiver.elem.isInstanceOf[CollElem[_, _]] && method.getName == "zip" =>
          val res = (receiver, args(0))
          Nullable(res).asInstanceOf[Nullable[(Rep[Coll[A]], Rep[Coll[B]]) forSome {type A; type B}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[Coll[A]], Rep[Coll[B]]) forSome {type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object foreach {
      def unapply(d: Def[_]): Nullable[(Rep[Coll[A]], Rep[A => Unit]) forSome {type A}] = d match {
        case MethodCall(receiver, method, args, _) if receiver.elem.isInstanceOf[CollElem[_, _]] && method.getName == "foreach" =>
          val res = (receiver, args(0))
          Nullable(res).asInstanceOf[Nullable[(Rep[Coll[A]], Rep[A => Unit]) forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[Coll[A]], Rep[A => Unit]) forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object exists {
      def unapply(d: Def[_]): Nullable[(Rep[Coll[A]], Rep[A => Boolean]) forSome {type A}] = d match {
        case MethodCall(receiver, method, args, _) if receiver.elem.isInstanceOf[CollElem[_, _]] && method.getName == "exists" =>
          val res = (receiver, args(0))
          Nullable(res).asInstanceOf[Nullable[(Rep[Coll[A]], Rep[A => Boolean]) forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[Coll[A]], Rep[A => Boolean]) forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object forall {
      def unapply(d: Def[_]): Nullable[(Rep[Coll[A]], Rep[A => Boolean]) forSome {type A}] = d match {
        case MethodCall(receiver, method, args, _) if receiver.elem.isInstanceOf[CollElem[_, _]] && method.getName == "forall" =>
          val res = (receiver, args(0))
          Nullable(res).asInstanceOf[Nullable[(Rep[Coll[A]], Rep[A => Boolean]) forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[Coll[A]], Rep[A => Boolean]) forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object filter {
      def unapply(d: Def[_]): Nullable[(Rep[Coll[A]], Rep[A => Boolean]) forSome {type A}] = d match {
        case MethodCall(receiver, method, args, _) if receiver.elem.isInstanceOf[CollElem[_, _]] && method.getName == "filter" =>
          val res = (receiver, args(0))
          Nullable(res).asInstanceOf[Nullable[(Rep[Coll[A]], Rep[A => Boolean]) forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[Coll[A]], Rep[A => Boolean]) forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object where {
      def unapply(d: Def[_]): Nullable[(Rep[Coll[A]], Rep[A => Boolean]) forSome {type A}] = d match {
        case MethodCall(receiver, method, args, _) if receiver.elem.isInstanceOf[CollElem[_, _]] && method.getName == "where" =>
          val res = (receiver, args(0))
          Nullable(res).asInstanceOf[Nullable[(Rep[Coll[A]], Rep[A => Boolean]) forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[Coll[A]], Rep[A => Boolean]) forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object fold {
      def unapply(d: Def[_]): Nullable[(Rep[Coll[A]], Rep[B], Rep[((B, A)) => B]) forSome {type A; type B}] = d match {
        case MethodCall(receiver, method, args, _) if receiver.elem.isInstanceOf[CollElem[_, _]] && method.getName == "fold" =>
          val res = (receiver, args(0), args(1))
          Nullable(res).asInstanceOf[Nullable[(Rep[Coll[A]], Rep[B], Rep[((B, A)) => B]) forSome {type A; type B}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[Coll[A]], Rep[B], Rep[((B, A)) => B]) forSome {type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object sum {
      def unapply(d: Def[_]): Nullable[(Rep[Coll[A]], Rep[Monoid[A]]) forSome {type A}] = d match {
        case MethodCall(receiver, method, args, _) if receiver.elem.isInstanceOf[CollElem[_, _]] && method.getName == "sum" =>
          val res = (receiver, args(0))
          Nullable(res).asInstanceOf[Nullable[(Rep[Coll[A]], Rep[Monoid[A]]) forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[Coll[A]], Rep[Monoid[A]]) forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object slice {
      def unapply(d: Def[_]): Nullable[(Rep[Coll[A]], Rep[Int], Rep[Int]) forSome {type A}] = d match {
        case MethodCall(receiver, method, args, _) if receiver.elem.isInstanceOf[CollElem[_, _]] && method.getName == "slice" =>
          val res = (receiver, args(0), args(1))
          Nullable(res).asInstanceOf[Nullable[(Rep[Coll[A]], Rep[Int], Rep[Int]) forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[Coll[A]], Rep[Int], Rep[Int]) forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object append {
      def unapply(d: Def[_]): Nullable[(Rep[Coll[A]], Rep[Coll[A]]) forSome {type A}] = d match {
        case MethodCall(receiver, method, args, _) if receiver.elem.isInstanceOf[CollElem[_, _]] && method.getName == "append" =>
          val res = (receiver, args(0))
          Nullable(res).asInstanceOf[Nullable[(Rep[Coll[A]], Rep[Coll[A]]) forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[Coll[A]], Rep[Coll[A]]) forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }
  }

  object CollCompanionMethods {
  }
} // of object Coll
  registerEntityObject("Coll", Coll)

  object UserTypeColl {
    def unapply(s: Sym): Option[Iso[_, _]] = {
      s.elem match {
        case e: CollElem[a,to] => e.eItem match {
          case UnpackableElem(iso) => Some(iso)
          case _ => None
        }
        case _ => None
      }
    }
  }

  override def unapplyViews[T](s: Exp[T]): Option[Unpacked[T]] = (s match {
    case Def(view: ViewColl[_, _]) =>
      Some((view.source, view.iso))
    case UserTypeColl(iso: Iso[a, b]) =>
      val newIso = collIso(iso)
      val repr = reifyObject(UnpackView(asRep[Coll[b]](s), newIso))
      Some((repr, newIso))
    case _ =>
      super.unapplyViews(s)
  }).asInstanceOf[Option[Unpacked[T]]]

  type RepColl[A] = Rep[Coll[A]]

  override def rewriteDef[T](d: Def[T]) = d match {
    case view1@ViewColl(Def(view2@ViewColl(arr, innerIso2)), innerIso1) =>
      val compIso = composeIso(innerIso1, innerIso2)
      implicit val eAB = compIso.eTo
      ViewColl(arr, compIso)

    case CollMethods.map(xs, f) => (xs, f) match {
      case (_, Def(IdentityLambda())) =>
        xs
      case (xs: RepColl[a] @unchecked, LambdaResultHasViews(f, iso: Iso[b, c])) =>
        val f1 = asRep[a => c](f)
        implicit val eB = iso.eFrom
        val s = xs.map(f1 >> iso.fromFun)
        val res = ViewColl(s, iso)
        res
      case (HasViews(source, Def(contIso: CollIso[a, b])), f: RFunc[_, c]@unchecked) =>
        val f1 = asRep[b => c](f)
        val iso = contIso.innerIso
        implicit val eC = f1.elem.eRange
        asRep[Coll[a]](source).map(iso.toFun >> f1)
      case _ =>
        super.rewriteDef(d)
    }
    case _ => super.rewriteDef(d)
  }

object PairColl extends EntityObject("PairColl") {
  // entityAdapter for PairColl trait
  case class PairCollAdapter[L, R](source: Rep[PairColl[L, R]])
      extends PairColl[L, R] with Def[PairColl[L, R]] {
    implicit lazy val eL = source.elem.typeArgs("L")._1.asElem[L];
implicit lazy val eR = source.elem.typeArgs("R")._1.asElem[R]
    override lazy val eA: Elem[(L, R)] = implicitly[Elem[(L, R)]]
    val selfType: Elem[PairColl[L, R]] = element[PairColl[L, R]]
    override def transform(t: Transformer) = PairCollAdapter[L, R](t(source))
    private val thisClass = classOf[PairColl[L, R]]

    def ls: Rep[Coll[L]] = {
      asRep[Coll[L]](mkMethodCall(source,
        thisClass.getMethod("ls"),
        List(),
        true, true, element[Coll[L]]))
    }

    def rs: Rep[Coll[R]] = {
      asRep[Coll[R]](mkMethodCall(source,
        thisClass.getMethod("rs"),
        List(),
        true, true, element[Coll[R]]))
    }

    def builder: Rep[ColBuilder] = {
      asRep[ColBuilder](mkMethodCall(source,
        thisClass.getMethod("builder"),
        List(),
        true, true, element[ColBuilder]))
    }

    def arr: Rep[WArray[(L, R)]] = {
      asRep[WArray[(L, R)]](mkMethodCall(source,
        thisClass.getMethod("arr"),
        List(),
        true, true, element[WArray[(L, R)]]))
    }

    def length: Rep[Int] = {
      asRep[Int](mkMethodCall(source,
        thisClass.getMethod("length"),
        List(),
        true, true, element[Int]))
    }

    def apply(i: Rep[Int]): Rep[(L, R)] = {
      asRep[(L, R)](mkMethodCall(source,
        thisClass.getMethod("apply", classOf[Sym]),
        List(i),
        true, true, element[(L, R)]))
    }

    def getOrElse(i: Rep[Int], default: Rep[(L, R)]): Rep[(L, R)] = {
      asRep[(L, R)](mkMethodCall(source,
        thisClass.getMethod("getOrElse", classOf[Sym], classOf[Sym]),
        List(i, default),
        true, true, element[(L, R)]))
    }

    def map[B](f: Rep[((L, R)) => B]): Rep[Coll[B]] = {
      implicit val eB = f.elem.eRange
      asRep[Coll[B]](mkMethodCall(source,
        thisClass.getMethod("map", classOf[Sym]),
        List(f),
        true, true, element[Coll[B]]))
    }

    def zip[B](ys: Rep[Coll[B]]): Rep[PairColl[(L, R), B]] = {
      implicit val eB = ys.eA
      asRep[PairColl[(L, R), B]](mkMethodCall(source,
        thisClass.getMethod("zip", classOf[Sym]),
        List(ys),
        true, true, element[PairColl[(L, R), B]]))
    }

    def foreach(f: Rep[((L, R)) => Unit]): Rep[Unit] = {
      asRep[Unit](mkMethodCall(source,
        thisClass.getMethod("foreach", classOf[Sym]),
        List(f),
        true, true, element[Unit]))
    }

    def exists(p: Rep[((L, R)) => Boolean]): Rep[Boolean] = {
      asRep[Boolean](mkMethodCall(source,
        thisClass.getMethod("exists", classOf[Sym]),
        List(p),
        true, true, element[Boolean]))
    }

    def forall(p: Rep[((L, R)) => Boolean]): Rep[Boolean] = {
      asRep[Boolean](mkMethodCall(source,
        thisClass.getMethod("forall", classOf[Sym]),
        List(p),
        true, true, element[Boolean]))
    }

    def filter(p: Rep[((L, R)) => Boolean]): Rep[Coll[(L, R)]] = {
      asRep[Coll[(L, R)]](mkMethodCall(source,
        thisClass.getMethod("filter", classOf[Sym]),
        List(p),
        true, true, element[Coll[(L, R)]]))
    }

    def fold[B](zero: Rep[B], op: Rep[((B, (L, R))) => B]): Rep[B] = {
      implicit val eB = zero.elem
      asRep[B](mkMethodCall(source,
        thisClass.getMethod("fold", classOf[Sym], classOf[Sym]),
        List(zero, op),
        true, true, element[B]))
    }

    def sum(m: Rep[Monoid[(L, R)]]): Rep[(L, R)] = {
      asRep[(L, R)](mkMethodCall(source,
        thisClass.getMethod("sum", classOf[Sym]),
        List(m),
        true, true, element[(L, R)]))
    }

    def slice(from: Rep[Int], until: Rep[Int]): Rep[Coll[(L, R)]] = {
      asRep[Coll[(L, R)]](mkMethodCall(source,
        thisClass.getMethod("slice", classOf[Sym], classOf[Sym]),
        List(from, until),
        true, true, element[Coll[(L, R)]]))
    }

    def append(other: Rep[Coll[(L, R)]]): Rep[Coll[(L, R)]] = {
      asRep[Coll[(L, R)]](mkMethodCall(source,
        thisClass.getMethod("append", classOf[Sym]),
        List(other),
        true, true, element[Coll[(L, R)]]))
    }
  }

  // entityProxy: single proxy for each type family
  implicit def proxyPairColl[L, R](p: Rep[PairColl[L, R]]): PairColl[L, R] = {
    if (p.rhs.isInstanceOf[PairColl[L, R]@unchecked]) p.rhs.asInstanceOf[PairColl[L, R]]
    else
      PairCollAdapter(p)
  }

  // familyElem
  class PairCollElem[L, R, To <: PairColl[L, R]](implicit _eL: Elem[L], _eR: Elem[R])
    extends CollElem[(L, R), To] {
    def eL = _eL
    def eR = _eR

    override lazy val parent: Option[Elem[_]] = Some(collElement(pairElement(element[L],element[R])))
    override def buildTypeArgs = super.buildTypeArgs ++ TypeArgs("L" -> (eL -> scalan.util.Invariant), "R" -> (eR -> scalan.util.Invariant))
    override lazy val tag = {
      implicit val tagL = eL.tag
      implicit val tagR = eR.tag
      weakTypeTag[PairColl[L, R]].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      val conv = fun {x: Rep[PairColl[L, R]] => convertPairColl(x) }
      tryConvert(element[PairColl[L, R]], this, x, conv)
    }

    def convertPairColl(x: Rep[PairColl[L, R]]): Rep[To] = {
      x.elem match {
        case _: PairCollElem[_, _, _] => asRep[To](x)
        case e => !!!(s"Expected $x to have PairCollElem[_, _, _], but got $e", x)
      }
    }
    override def getDefaultRep: Rep[To] = ???
  }

  implicit def pairCollElement[L, R](implicit eL: Elem[L], eR: Elem[R]): Elem[PairColl[L, R]] =
    cachedElem[PairCollElem[L, R, PairColl[L, R]]](eL, eR)

  implicit case object PairCollCompanionElem extends CompanionElem[PairCollCompanionCtor] {
    lazy val tag = weakTypeTag[PairCollCompanionCtor]
    protected def getDefaultRep = RPairColl
  }

  abstract class PairCollCompanionCtor extends CompanionDef[PairCollCompanionCtor] with PairCollCompanion {
    def selfType = PairCollCompanionElem
    override def toString = "PairColl"
  }
  implicit def proxyPairCollCompanionCtor(p: Rep[PairCollCompanionCtor]): PairCollCompanionCtor =
    proxyOps[PairCollCompanionCtor](p)

  lazy val RPairColl: Rep[PairCollCompanionCtor] = new PairCollCompanionCtor {
    private val thisClass = classOf[PairCollCompanion]
  }

  object PairCollMethods {
    object ls {
      def unapply(d: Def[_]): Nullable[Rep[PairColl[L, R]] forSome {type L; type R}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[PairCollElem[_, _, _]] && method.getName == "ls" =>
          val res = receiver
          Nullable(res).asInstanceOf[Nullable[Rep[PairColl[L, R]] forSome {type L; type R}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[Rep[PairColl[L, R]] forSome {type L; type R}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object rs {
      def unapply(d: Def[_]): Nullable[Rep[PairColl[L, R]] forSome {type L; type R}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[PairCollElem[_, _, _]] && method.getName == "rs" =>
          val res = receiver
          Nullable(res).asInstanceOf[Nullable[Rep[PairColl[L, R]] forSome {type L; type R}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[Rep[PairColl[L, R]] forSome {type L; type R}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }
  }

  object PairCollCompanionMethods {
  }
} // of object PairColl
  registerEntityObject("PairColl", PairColl)

object ReplColl extends EntityObject("ReplColl") {
  // entityConst: single const for each entity
  import Liftables._
  import scala.reflect.{ClassTag, classTag}
  type SReplColl[A] = special.collection.ReplColl[A]
  case class ReplCollConst[SA, A](
        constValue: SReplColl[SA],
        lA: Liftable[SA, A]
      ) extends ReplColl[A] with LiftedConst[SReplColl[SA], ReplColl[A]]
        with Def[ReplColl[A]] with ReplCollConstMethods[A] {
    implicit def eA: Elem[A] = lA.eW

    val liftable: Liftable[SReplColl[SA], ReplColl[A]] = liftableReplColl(lA)
    val selfType: Elem[ReplColl[A]] = liftable.eW
  }

  trait ReplCollConstMethods[A] extends ReplColl[A] with CollConstMethods[A] { thisConst: Def[_] =>
    implicit def eA: Elem[A]
    private val ReplCollClass = classOf[ReplColl[A]]

    override def value: Rep[A] = {
      asRep[A](mkMethodCall(self,
        ReplCollClass.getMethod("value"),
        List(),
        true, false, element[A]))
    }

    override def length: Rep[Int] = {
      asRep[Int](mkMethodCall(self,
        ReplCollClass.getMethod("length"),
        List(),
        true, false, element[Int]))
    }

    override def append(other: Rep[Coll[A]]): Rep[Coll[A]] = {
      asRep[Coll[A]](mkMethodCall(self,
        ReplCollClass.getMethod("append", classOf[Sym]),
        List(other),
        true, false, element[Coll[A]]))
    }
  }

  case class LiftableReplColl[SA, A](lA: Liftable[SA, A])
    extends Liftable[SReplColl[SA], ReplColl[A]] {
    lazy val eW: Elem[ReplColl[A]] = replCollElement(lA.eW)
    lazy val sourceClassTag: ClassTag[SReplColl[SA]] = {
            implicit val tagSA = lA.eW.sourceClassTag.asInstanceOf[ClassTag[SA]]
      classTag[SReplColl[SA]]
    }
    def lift(x: SReplColl[SA]): Rep[ReplColl[A]] = ReplCollConst(x, lA)
    def unlift(w: Rep[ReplColl[A]]): SReplColl[SA] = w match {
      case Def(ReplCollConst(x: SReplColl[_], _lA))
            if _lA == lA => x.asInstanceOf[SReplColl[SA]]
      case _ => unliftError(w)
    }
  }
  implicit def liftableReplColl[SA, A](implicit lA: Liftable[SA,A]): Liftable[SReplColl[SA], ReplColl[A]] =
    LiftableReplColl(lA)

  // entityAdapter for ReplColl trait
  case class ReplCollAdapter[A](source: Rep[ReplColl[A]])
      extends ReplColl[A] with Def[ReplColl[A]] {
    implicit lazy val eA = source.elem.typeArgs("A")._1.asElem[A]

    val selfType: Elem[ReplColl[A]] = element[ReplColl[A]]
    override def transform(t: Transformer) = ReplCollAdapter[A](t(source))
    private val thisClass = classOf[ReplColl[A]]

    def value: Rep[A] = {
      asRep[A](mkMethodCall(source,
        thisClass.getMethod("value"),
        List(),
        true, true, element[A]))
    }

    def length: Rep[Int] = {
      asRep[Int](mkMethodCall(source,
        thisClass.getMethod("length"),
        List(),
        true, true, element[Int]))
    }

    def append(other: Rep[Coll[A]]): Rep[Coll[A]] = {
      asRep[Coll[A]](mkMethodCall(source,
        thisClass.getMethod("append", classOf[Sym]),
        List(other),
        true, true, element[Coll[A]]))
    }

    def builder: Rep[ColBuilder] = {
      asRep[ColBuilder](mkMethodCall(source,
        thisClass.getMethod("builder"),
        List(),
        true, true, element[ColBuilder]))
    }

    def arr: Rep[WArray[A]] = {
      asRep[WArray[A]](mkMethodCall(source,
        thisClass.getMethod("arr"),
        List(),
        true, true, element[WArray[A]]))
    }

    def apply(i: Rep[Int]): Rep[A] = {
      asRep[A](mkMethodCall(source,
        thisClass.getMethod("apply", classOf[Sym]),
        List(i),
        true, true, element[A]))
    }

    def getOrElse(i: Rep[Int], default: Rep[A]): Rep[A] = {
      asRep[A](mkMethodCall(source,
        thisClass.getMethod("getOrElse", classOf[Sym], classOf[Sym]),
        List(i, default),
        true, true, element[A]))
    }

    def map[B](f: Rep[A => B]): Rep[Coll[B]] = {
      implicit val eB = f.elem.eRange
      asRep[Coll[B]](mkMethodCall(source,
        thisClass.getMethod("map", classOf[Sym]),
        List(f),
        true, true, element[Coll[B]]))
    }

    def zip[B](ys: Rep[Coll[B]]): Rep[PairColl[A, B]] = {
      implicit val eB = ys.eA
      asRep[PairColl[A, B]](mkMethodCall(source,
        thisClass.getMethod("zip", classOf[Sym]),
        List(ys),
        true, true, element[PairColl[A, B]]))
    }

    def foreach(f: Rep[A => Unit]): Rep[Unit] = {
      asRep[Unit](mkMethodCall(source,
        thisClass.getMethod("foreach", classOf[Sym]),
        List(f),
        true, true, element[Unit]))
    }

    def exists(p: Rep[A => Boolean]): Rep[Boolean] = {
      asRep[Boolean](mkMethodCall(source,
        thisClass.getMethod("exists", classOf[Sym]),
        List(p),
        true, true, element[Boolean]))
    }

    def forall(p: Rep[A => Boolean]): Rep[Boolean] = {
      asRep[Boolean](mkMethodCall(source,
        thisClass.getMethod("forall", classOf[Sym]),
        List(p),
        true, true, element[Boolean]))
    }

    def filter(p: Rep[A => Boolean]): Rep[Coll[A]] = {
      asRep[Coll[A]](mkMethodCall(source,
        thisClass.getMethod("filter", classOf[Sym]),
        List(p),
        true, true, element[Coll[A]]))
    }

    def fold[B](zero: Rep[B], op: Rep[((B, A)) => B]): Rep[B] = {
      implicit val eB = zero.elem
      asRep[B](mkMethodCall(source,
        thisClass.getMethod("fold", classOf[Sym], classOf[Sym]),
        List(zero, op),
        true, true, element[B]))
    }

    def sum(m: Rep[Monoid[A]]): Rep[A] = {
      asRep[A](mkMethodCall(source,
        thisClass.getMethod("sum", classOf[Sym]),
        List(m),
        true, true, element[A]))
    }

    def slice(from: Rep[Int], until: Rep[Int]): Rep[Coll[A]] = {
      asRep[Coll[A]](mkMethodCall(source,
        thisClass.getMethod("slice", classOf[Sym], classOf[Sym]),
        List(from, until),
        true, true, element[Coll[A]]))
    }
  }

  // entityProxy: single proxy for each type family
  implicit def proxyReplColl[A](p: Rep[ReplColl[A]]): ReplColl[A] = {
    if (p.rhs.isInstanceOf[ReplColl[A]@unchecked]) p.rhs.asInstanceOf[ReplColl[A]]
    else
      ReplCollAdapter(p)
  }

  // familyElem
  class ReplCollElem[A, To <: ReplColl[A]](implicit _eA: Elem[A])
    extends CollElem[A, To] {
    override def eA = _eA

    override val liftable: Liftables.Liftable[_, To] = liftableReplColl(_eA.liftable).asLiftable[SReplColl[_], To]

    override protected def collectMethods: Map[java.lang.reflect.Method, MethodDesc] = {
      super.collectMethods ++
        Elem.declaredMethods(classOf[ReplColl[A]], classOf[SReplColl[_]], Set(
        "value", "length", "append"
        ))
    }

    override lazy val parent: Option[Elem[_]] = Some(collElement(element[A]))
    override def buildTypeArgs = super.buildTypeArgs ++ TypeArgs("A" -> (eA -> scalan.util.Invariant))
    override lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[ReplColl[A]].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      val conv = fun {x: Rep[ReplColl[A]] => convertReplColl(x) }
      tryConvert(element[ReplColl[A]], this, x, conv)
    }

    def convertReplColl(x: Rep[ReplColl[A]]): Rep[To] = {
      x.elem match {
        case _: ReplCollElem[_, _] => asRep[To](x)
        case e => !!!(s"Expected $x to have ReplCollElem[_, _], but got $e", x)
      }
    }
    override def getDefaultRep: Rep[To] = ???
  }

  implicit def replCollElement[A](implicit eA: Elem[A]): Elem[ReplColl[A]] =
    cachedElem[ReplCollElem[A, ReplColl[A]]](eA)

  implicit case object ReplCollCompanionElem extends CompanionElem[ReplCollCompanionCtor] {
    lazy val tag = weakTypeTag[ReplCollCompanionCtor]
    protected def getDefaultRep = RReplColl
  }

  abstract class ReplCollCompanionCtor extends CompanionDef[ReplCollCompanionCtor] with ReplCollCompanion {
    def selfType = ReplCollCompanionElem
    override def toString = "ReplColl"
  }
  implicit def proxyReplCollCompanionCtor(p: Rep[ReplCollCompanionCtor]): ReplCollCompanionCtor =
    proxyOps[ReplCollCompanionCtor](p)

  lazy val RReplColl: Rep[ReplCollCompanionCtor] = new ReplCollCompanionCtor {
    private val thisClass = classOf[ReplCollCompanion]
  }

  object ReplCollMethods {
    object value {
      def unapply(d: Def[_]): Nullable[Rep[ReplColl[A]] forSome {type A}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[ReplCollElem[_, _]] && method.getName == "value" =>
          val res = receiver
          Nullable(res).asInstanceOf[Nullable[Rep[ReplColl[A]] forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[Rep[ReplColl[A]] forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object length {
      def unapply(d: Def[_]): Nullable[Rep[ReplColl[A]] forSome {type A}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[ReplCollElem[_, _]] && method.getName == "length" =>
          val res = receiver
          Nullable(res).asInstanceOf[Nullable[Rep[ReplColl[A]] forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[Rep[ReplColl[A]] forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object append {
      def unapply(d: Def[_]): Nullable[(Rep[ReplColl[A]], Rep[Coll[A]]) forSome {type A}] = d match {
        case MethodCall(receiver, method, args, _) if receiver.elem.isInstanceOf[ReplCollElem[_, _]] && method.getName == "append" =>
          val res = (receiver, args(0))
          Nullable(res).asInstanceOf[Nullable[(Rep[ReplColl[A]], Rep[Coll[A]]) forSome {type A}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[ReplColl[A]], Rep[Coll[A]]) forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }
  }

  object ReplCollCompanionMethods {
  }
} // of object ReplColl
  registerEntityObject("ReplColl", ReplColl)

object ColBuilder extends EntityObject("ColBuilder") {
  // entityConst: single const for each entity
  import Liftables._
  import scala.reflect.{ClassTag, classTag}
  type SColBuilder = special.collection.CollBuilder
  case class ColBuilderConst(
        constValue: SColBuilder
      ) extends ColBuilder with LiftedConst[SColBuilder, ColBuilder]
        with Def[ColBuilder] with ColBuilderConstMethods {
    val liftable: Liftable[SColBuilder, ColBuilder] = LiftableColBuilder
    val selfType: Elem[ColBuilder] = liftable.eW
  }

  trait ColBuilderConstMethods extends ColBuilder  { thisConst: Def[_] =>

    private val ColBuilderClass = classOf[ColBuilder]

    override def pairCol[A, B](as: Rep[Coll[A]], bs: Rep[Coll[B]]): Rep[PairColl[A, B]] = {
      implicit val eA = as.eA
implicit val eB = bs.eA
      asRep[PairColl[A, B]](mkMethodCall(self,
        ColBuilderClass.getMethod("pairCol", classOf[Sym], classOf[Sym]),
        List(as, bs),
        true, false, element[PairColl[A, B]]))
    }

    override def fromItems[T](items: Rep[T]*)(implicit cT: Elem[T]): Rep[Coll[T]] = {
      asRep[Coll[T]](mkMethodCall(self,
        ColBuilderClass.getMethod("fromItems", classOf[Seq[_]], classOf[Elem[_]]),
        List(items, cT),
        true, false, element[Coll[T]]))
    }

    override def xor(left: Rep[Coll[Byte]], right: Rep[Coll[Byte]]): Rep[Coll[Byte]] = {
      asRep[Coll[Byte]](mkMethodCall(self,
        ColBuilderClass.getMethod("xor", classOf[Sym], classOf[Sym]),
        List(left, right),
        true, false, element[Coll[Byte]]))
    }

    override def fromArray[T](arr: Rep[WArray[T]]): Rep[Coll[T]] = {
      implicit val eT = arr.eT
      asRep[Coll[T]](mkMethodCall(self,
        ColBuilderClass.getMethod("fromArray", classOf[Sym]),
        List(arr),
        true, false, element[Coll[T]]))
    }

    override def replicate[T](n: Rep[Int], v: Rep[T]): Rep[Coll[T]] = {
      implicit val eT = v.elem
      asRep[Coll[T]](mkMethodCall(self,
        ColBuilderClass.getMethod("replicate", classOf[Sym], classOf[Sym]),
        List(n, v),
        true, false, element[Coll[T]]))
    }
  }

  implicit object LiftableColBuilder
    extends Liftable[SColBuilder, ColBuilder] {
    lazy val eW: Elem[ColBuilder] = colBuilderElement
    lazy val sourceClassTag: ClassTag[SColBuilder] = {
      classTag[SColBuilder]
    }
    def lift(x: SColBuilder): Rep[ColBuilder] = ColBuilderConst(x)
    def unlift(w: Rep[ColBuilder]): SColBuilder = w match {
      case Def(ColBuilderConst(x: SColBuilder))
            => x.asInstanceOf[SColBuilder]
      case _ => unliftError(w)
    }
  }

  // entityAdapter for ColBuilder trait
  case class ColBuilderAdapter(source: Rep[ColBuilder])
      extends ColBuilder with Def[ColBuilder] {
    val selfType: Elem[ColBuilder] = element[ColBuilder]
    override def transform(t: Transformer) = ColBuilderAdapter(t(source))
    private val thisClass = classOf[ColBuilder]

    def pairCol[A, B](as: Rep[Coll[A]], bs: Rep[Coll[B]]): Rep[PairColl[A, B]] = {
      implicit val eA = as.eA
implicit val eB = bs.eA
      asRep[PairColl[A, B]](mkMethodCall(source,
        thisClass.getMethod("pairCol", classOf[Sym], classOf[Sym]),
        List(as, bs),
        true, true, element[PairColl[A, B]]))
    }

    def fromItems[T](items: Rep[T]*)(implicit cT: Elem[T]): Rep[Coll[T]] = {
      asRep[Coll[T]](mkMethodCall(source,
        thisClass.getMethod("fromItems", classOf[Seq[_]], classOf[Elem[_]]),
        List(items, cT),
        true, true, element[Coll[T]]))
    }

    def xor(left: Rep[Coll[Byte]], right: Rep[Coll[Byte]]): Rep[Coll[Byte]] = {
      asRep[Coll[Byte]](mkMethodCall(source,
        thisClass.getMethod("xor", classOf[Sym], classOf[Sym]),
        List(left, right),
        true, true, element[Coll[Byte]]))
    }

    def fromArray[T](arr: Rep[WArray[T]]): Rep[Coll[T]] = {
      implicit val eT = arr.eT
      asRep[Coll[T]](mkMethodCall(source,
        thisClass.getMethod("fromArray", classOf[Sym]),
        List(arr),
        true, true, element[Coll[T]]))
    }

    def replicate[T](n: Rep[Int], v: Rep[T]): Rep[Coll[T]] = {
      implicit val eT = v.elem
      asRep[Coll[T]](mkMethodCall(source,
        thisClass.getMethod("replicate", classOf[Sym], classOf[Sym]),
        List(n, v),
        true, true, element[Coll[T]]))
    }
  }

  // entityProxy: single proxy for each type family
  implicit def proxyColBuilder(p: Rep[ColBuilder]): ColBuilder = {
    if (p.rhs.isInstanceOf[ColBuilder@unchecked]) p.rhs.asInstanceOf[ColBuilder]
    else
      ColBuilderAdapter(p)
  }

  // familyElem
  class ColBuilderElem[To <: ColBuilder]
    extends EntityElem[To] {
    override val liftable: Liftables.Liftable[_, To] = LiftableColBuilder.asLiftable[SColBuilder, To]

    override protected def collectMethods: Map[java.lang.reflect.Method, MethodDesc] = {
      super.collectMethods ++
        Elem.declaredMethods(classOf[ColBuilder], classOf[SColBuilder], Set(
        "pairCol", "fromItems", "unzip", "xor", "fromArray", "replicate"
        ))
    }

    lazy val parent: Option[Elem[_]] = None
    override def buildTypeArgs = super.buildTypeArgs ++ TypeArgs()
    override lazy val tag = {
      weakTypeTag[ColBuilder].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      val conv = fun {x: Rep[ColBuilder] => convertColBuilder(x) }
      tryConvert(element[ColBuilder], this, x, conv)
    }

    def convertColBuilder(x: Rep[ColBuilder]): Rep[To] = {
      x.elem match {
        case _: ColBuilderElem[_] => asRep[To](x)
        case e => !!!(s"Expected $x to have ColBuilderElem[_], but got $e", x)
      }
    }
    override def getDefaultRep: Rep[To] = ???
  }

  implicit lazy val colBuilderElement: Elem[ColBuilder] =
    new ColBuilderElem[ColBuilder]

  implicit case object ColBuilderCompanionElem extends CompanionElem[ColBuilderCompanionCtor] {
    lazy val tag = weakTypeTag[ColBuilderCompanionCtor]
    protected def getDefaultRep = RColBuilder
  }

  abstract class ColBuilderCompanionCtor extends CompanionDef[ColBuilderCompanionCtor] with ColBuilderCompanion {
    def selfType = ColBuilderCompanionElem
    override def toString = "ColBuilder"
  }
  implicit def proxyColBuilderCompanionCtor(p: Rep[ColBuilderCompanionCtor]): ColBuilderCompanionCtor =
    proxyOps[ColBuilderCompanionCtor](p)

  lazy val RColBuilder: Rep[ColBuilderCompanionCtor] = new ColBuilderCompanionCtor {
    private val thisClass = classOf[ColBuilderCompanion]
  }

  object ColBuilderMethods {
    object pairCol {
      def unapply(d: Def[_]): Nullable[(Rep[ColBuilder], Rep[Coll[A]], Rep[Coll[B]]) forSome {type A; type B}] = d match {
        case MethodCall(receiver, method, args, _) if receiver.elem.isInstanceOf[ColBuilderElem[_]] && method.getName == "pairCol" =>
          val res = (receiver, args(0), args(1))
          Nullable(res).asInstanceOf[Nullable[(Rep[ColBuilder], Rep[Coll[A]], Rep[Coll[B]]) forSome {type A; type B}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[ColBuilder], Rep[Coll[A]], Rep[Coll[B]]) forSome {type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object fromItems {
      def unapply(d: Def[_]): Nullable[(Rep[ColBuilder], Seq[Rep[T]], Elem[T]) forSome {type T}] = d match {
        case MethodCall(receiver, method, args, _) if receiver.elem.isInstanceOf[ColBuilderElem[_]] && method.getName == "fromItems" =>
          val res = (receiver, args(0), args(1))
          Nullable(res).asInstanceOf[Nullable[(Rep[ColBuilder], Seq[Rep[T]], Elem[T]) forSome {type T}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[ColBuilder], Seq[Rep[T]], Elem[T]) forSome {type T}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object unzip {
      def unapply(d: Def[_]): Nullable[(Rep[ColBuilder], Rep[Coll[(A, B)]]) forSome {type A; type B}] = d match {
        case MethodCall(receiver, method, args, _) if receiver.elem.isInstanceOf[ColBuilderElem[_]] && method.getName == "unzip" =>
          val res = (receiver, args(0))
          Nullable(res).asInstanceOf[Nullable[(Rep[ColBuilder], Rep[Coll[(A, B)]]) forSome {type A; type B}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[ColBuilder], Rep[Coll[(A, B)]]) forSome {type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object xor {
      def unapply(d: Def[_]): Nullable[(Rep[ColBuilder], Rep[Coll[Byte]], Rep[Coll[Byte]])] = d match {
        case MethodCall(receiver, method, args, _) if receiver.elem.isInstanceOf[ColBuilderElem[_]] && method.getName == "xor" =>
          val res = (receiver, args(0), args(1))
          Nullable(res).asInstanceOf[Nullable[(Rep[ColBuilder], Rep[Coll[Byte]], Rep[Coll[Byte]])]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[ColBuilder], Rep[Coll[Byte]], Rep[Coll[Byte]])] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object fromArray {
      def unapply(d: Def[_]): Nullable[(Rep[ColBuilder], Rep[WArray[T]]) forSome {type T}] = d match {
        case MethodCall(receiver, method, args, _) if receiver.elem.isInstanceOf[ColBuilderElem[_]] && method.getName == "fromArray" =>
          val res = (receiver, args(0))
          Nullable(res).asInstanceOf[Nullable[(Rep[ColBuilder], Rep[WArray[T]]) forSome {type T}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[ColBuilder], Rep[WArray[T]]) forSome {type T}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }

    object replicate {
      def unapply(d: Def[_]): Nullable[(Rep[ColBuilder], Rep[Int], Rep[T]) forSome {type T}] = d match {
        case MethodCall(receiver, method, args, _) if receiver.elem.isInstanceOf[ColBuilderElem[_]] && method.getName == "replicate" =>
          val res = (receiver, args(0), args(1))
          Nullable(res).asInstanceOf[Nullable[(Rep[ColBuilder], Rep[Int], Rep[T]) forSome {type T}]]
        case _ => Nullable.None
      }
      def unapply(exp: Sym): Nullable[(Rep[ColBuilder], Rep[Int], Rep[T]) forSome {type T}] = exp match {
        case Def(d) => unapply(d)
        case _ => Nullable.None
      }
    }
  }

  object ColBuilderCompanionMethods {
  }
} // of object ColBuilder
  registerEntityObject("ColBuilder", ColBuilder)

  registerModule(CollsModule)
}

object CollsModule extends scalan.ModuleInfo("special.collection", "Colls")
}

trait CollsModule extends special.collection.impl.CollsDefs {self: Library =>}
