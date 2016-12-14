package scalan.monads

import scalan._
import scala.reflect.runtime.universe._
import scala.reflect.runtime.universe.{WeakTypeTag, weakTypeTag}
import scalan.meta.ScalanAst._

package impl {
// Abs -----------------------------------
trait FreesAbs extends scalan.ScalanDsl with Frees {
  self: MonadsDsl =>

  // single proxy for each type family
  implicit def proxyFree[F[_], A](p: Rep[Free[F, A]]): Free[F, A] = {
    proxyOps[Free[F, A]](p)(scala.reflect.classTag[Free[F, A]])
  }

  // familyElem
  class FreeElem[F[_], A, To <: Free[F, A]](implicit _cF: Cont[F], _eA: Elem[A])
    extends EntityElem[To] {
    def cF = _cF
    def eA = _eA
    lazy val parent: Option[Elem[_]] = None
    lazy val typeArgs = TypeArgs("F" -> (cF -> scalan.util.Invariant), "A" -> (eA -> scalan.util.Invariant))
    override def isEntityType = true
    override lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[Free[F, A]].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      implicit val eTo: Elem[To] = this
      val conv = fun {x: Rep[Free[F, A]] => convertFree(x) }
      tryConvert(element[Free[F, A]], this, x, conv)
    }

    def convertFree(x: Rep[Free[F, A]]): Rep[To] = {
      x.selfType1.asInstanceOf[Elem[_]] match {
        case _: FreeElem[_, _, _] => x.asRep[To]
        case e => !!!(s"Expected $x to have FreeElem[_, _, _], but got $e", x)
      }
    }

    override def getDefaultRep: Rep[To] = ???
  }

  implicit def freeElement[F[_], A](implicit cF: Cont[F], eA: Elem[A]): Elem[Free[F, A]] =
    cachedElem[FreeElem[F, A, Free[F, A]]](cF, eA)

  implicit case object FreeCompanionElem extends CompanionElem[FreeCompanionAbs] {
    lazy val tag = weakTypeTag[FreeCompanionAbs]
    protected def getDefaultRep = Free
  }

  abstract class FreeCompanionAbs extends CompanionDef[FreeCompanionAbs] with FreeCompanion {
    def selfType = FreeCompanionElem
    override def toString = "Free"
  }
  def Free: Rep[FreeCompanionAbs]
  implicit def proxyFreeCompanionAbs(p: Rep[FreeCompanionAbs]): FreeCompanionAbs =
    proxyOps[FreeCompanionAbs](p)

  abstract class AbsReturn[F[_], A]
      (a: Rep[A])(implicit eA: Elem[A], cF: Cont[F])
    extends Return[F, A](a) with Def[Return[F, A]] {
    lazy val selfType = element[Return[F, A]]
  }
  // elem for concrete class
  class ReturnElem[F[_], A](val iso: Iso[ReturnData[F, A], Return[F, A]])(implicit override val eA: Elem[A], override val cF: Cont[F])
    extends FreeElem[F, A, Return[F, A]]
    with ConcreteElem[ReturnData[F, A], Return[F, A]] {
    override lazy val parent: Option[Elem[_]] = Some(freeElement(container[F], element[A]))
    override lazy val typeArgs = TypeArgs("F" -> (cF -> scalan.util.Invariant), "A" -> (eA -> scalan.util.Invariant))

    override def convertFree(x: Rep[Free[F, A]]) = // Converter is not generated by meta
!!!("Cannot convert from Free to Return: missing fields List(a)")
    override def getDefaultRep = Return(element[A].defaultRepValue)
    override lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[Return[F, A]]
    }
  }

  // state representation type
  type ReturnData[F[_], A] = A

  // 3) Iso for concrete class
  class ReturnIso[F[_], A](implicit eA: Elem[A], cF: Cont[F])
    extends EntityIso[ReturnData[F, A], Return[F, A]] with Def[ReturnIso[F, A]] {
    override def from(p: Rep[Return[F, A]]) =
      p.a
    override def to(p: Rep[A]) = {
      val a = p
      Return(a)
    }
    lazy val eFrom = element[A]
    lazy val eTo = new ReturnElem[F, A](self)
    lazy val selfType = new ReturnIsoElem[F, A](eA, cF)
    def productArity = 2
    def productElement(n: Int) = n match {
      case 0 => eA
      case 1 => cF
    }
  }
  case class ReturnIsoElem[F[_], A](eA: Elem[A], cF: Cont[F]) extends Elem[ReturnIso[F, A]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new ReturnIso[F, A]()(eA, cF))
    lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[ReturnIso[F, A]]
    }
    lazy val typeArgs = TypeArgs("F" -> (cF -> scalan.util.Invariant), "A" -> (eA -> scalan.util.Invariant))
  }
  // 4) constructor and deconstructor
  class ReturnCompanionAbs extends CompanionDef[ReturnCompanionAbs] with ReturnCompanion {
    def selfType = ReturnCompanionElem
    override def toString = "Return"

    @scalan.OverloadId("fromFields")
    def apply[F[_], A](a: Rep[A])(implicit eA: Elem[A], cF: Cont[F]): Rep[Return[F, A]] =
      mkReturn(a)

    def unapply[F[_], A](p: Rep[Free[F, A]]) = unmkReturn(p)
  }
  lazy val ReturnRep: Rep[ReturnCompanionAbs] = new ReturnCompanionAbs
  lazy val Return: ReturnCompanionAbs = proxyReturnCompanion(ReturnRep)
  implicit def proxyReturnCompanion(p: Rep[ReturnCompanionAbs]): ReturnCompanionAbs = {
    proxyOps[ReturnCompanionAbs](p)
  }

  implicit case object ReturnCompanionElem extends CompanionElem[ReturnCompanionAbs] {
    lazy val tag = weakTypeTag[ReturnCompanionAbs]
    protected def getDefaultRep = Return
  }

  implicit def proxyReturn[F[_], A](p: Rep[Return[F, A]]): Return[F, A] =
    proxyOps[Return[F, A]](p)

  implicit class ExtendedReturn[F[_], A](p: Rep[Return[F, A]])(implicit eA: Elem[A], cF: Cont[F]) {
    def toData: Rep[ReturnData[F, A]] = isoReturn(eA, cF).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoReturn[F[_], A](implicit eA: Elem[A], cF: Cont[F]): Iso[ReturnData[F, A], Return[F, A]] =
    reifyObject(new ReturnIso[F, A]()(eA, cF))

  // 6) smart constructor and deconstructor
  def mkReturn[F[_], A](a: Rep[A])(implicit eA: Elem[A], cF: Cont[F]): Rep[Return[F, A]]
  def unmkReturn[F[_], A](p: Rep[Free[F, A]]): Option[(Rep[A])]

  abstract class AbsSuspend[F[_], A]
      (a: Rep[F[A]])(implicit eA: Elem[A], cF: Cont[F])
    extends Suspend[F, A](a) with Def[Suspend[F, A]] {
    lazy val selfType = element[Suspend[F, A]]
  }
  // elem for concrete class
  class SuspendElem[F[_], A](val iso: Iso[SuspendData[F, A], Suspend[F, A]])(implicit override val eA: Elem[A], override val cF: Cont[F])
    extends FreeElem[F, A, Suspend[F, A]]
    with ConcreteElem[SuspendData[F, A], Suspend[F, A]] {
    override lazy val parent: Option[Elem[_]] = Some(freeElement(container[F], element[A]))
    override lazy val typeArgs = TypeArgs("F" -> (cF -> scalan.util.Invariant), "A" -> (eA -> scalan.util.Invariant))

    override def convertFree(x: Rep[Free[F, A]]) = // Converter is not generated by meta
!!!("Cannot convert from Free to Suspend: missing fields List(a)")
    override def getDefaultRep = Suspend(cF.lift(eA).defaultRepValue)
    override lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[Suspend[F, A]]
    }
  }

  // state representation type
  type SuspendData[F[_], A] = F[A]

  // 3) Iso for concrete class
  class SuspendIso[F[_], A](implicit eA: Elem[A], cF: Cont[F])
    extends EntityIso[SuspendData[F, A], Suspend[F, A]] with Def[SuspendIso[F, A]] {
    override def from(p: Rep[Suspend[F, A]]) =
      p.a
    override def to(p: Rep[F[A]]) = {
      val a = p
      Suspend(a)
    }
    lazy val eFrom = element[F[A]]
    lazy val eTo = new SuspendElem[F, A](self)
    lazy val selfType = new SuspendIsoElem[F, A](eA, cF)
    def productArity = 2
    def productElement(n: Int) = n match {
      case 0 => eA
      case 1 => cF
    }
  }
  case class SuspendIsoElem[F[_], A](eA: Elem[A], cF: Cont[F]) extends Elem[SuspendIso[F, A]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new SuspendIso[F, A]()(eA, cF))
    lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[SuspendIso[F, A]]
    }
    lazy val typeArgs = TypeArgs("F" -> (cF -> scalan.util.Invariant), "A" -> (eA -> scalan.util.Invariant))
  }
  // 4) constructor and deconstructor
  class SuspendCompanionAbs extends CompanionDef[SuspendCompanionAbs] with SuspendCompanion {
    def selfType = SuspendCompanionElem
    override def toString = "Suspend"

    @scalan.OverloadId("fromFields")
    def apply[F[_], A](a: Rep[F[A]])(implicit eA: Elem[A], cF: Cont[F]): Rep[Suspend[F, A]] =
      mkSuspend(a)

    def unapply[F[_], A](p: Rep[Free[F, A]]) = unmkSuspend(p)
  }
  lazy val SuspendRep: Rep[SuspendCompanionAbs] = new SuspendCompanionAbs
  lazy val Suspend: SuspendCompanionAbs = proxySuspendCompanion(SuspendRep)
  implicit def proxySuspendCompanion(p: Rep[SuspendCompanionAbs]): SuspendCompanionAbs = {
    proxyOps[SuspendCompanionAbs](p)
  }

  implicit case object SuspendCompanionElem extends CompanionElem[SuspendCompanionAbs] {
    lazy val tag = weakTypeTag[SuspendCompanionAbs]
    protected def getDefaultRep = Suspend
  }

  implicit def proxySuspend[F[_], A](p: Rep[Suspend[F, A]]): Suspend[F, A] =
    proxyOps[Suspend[F, A]](p)

  implicit class ExtendedSuspend[F[_], A](p: Rep[Suspend[F, A]])(implicit eA: Elem[A], cF: Cont[F]) {
    def toData: Rep[SuspendData[F, A]] = isoSuspend(eA, cF).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoSuspend[F[_], A](implicit eA: Elem[A], cF: Cont[F]): Iso[SuspendData[F, A], Suspend[F, A]] =
    reifyObject(new SuspendIso[F, A]()(eA, cF))

  // 6) smart constructor and deconstructor
  def mkSuspend[F[_], A](a: Rep[F[A]])(implicit eA: Elem[A], cF: Cont[F]): Rep[Suspend[F, A]]
  def unmkSuspend[F[_], A](p: Rep[Free[F, A]]): Option[(Rep[F[A]])]

  abstract class AbsBind[F[_], S, B]
      (a: Rep[Free[F, S]], f: Rep[S => Free[F, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F])
    extends Bind[F, S, B](a, f) with Def[Bind[F, S, B]] {
    lazy val selfType = element[Bind[F, S, B]]
  }
  // elem for concrete class
  class BindElem[F[_], S, B](val iso: Iso[BindData[F, S, B], Bind[F, S, B]])(implicit val eS: Elem[S], override val eA: Elem[B], override val cF: Cont[F])
    extends FreeElem[F, B, Bind[F, S, B]]
    with ConcreteElem[BindData[F, S, B], Bind[F, S, B]] {
    override lazy val parent: Option[Elem[_]] = Some(freeElement(container[F], element[B]))
    override lazy val typeArgs = TypeArgs("F" -> (cF -> scalan.util.Invariant), "S" -> (eS -> scalan.util.Invariant), "B" -> (eA -> scalan.util.Invariant))

    override def convertFree(x: Rep[Free[F, B]]) = // Converter is not generated by meta
!!!("Cannot convert from Free to Bind: missing fields List(a, f)")
    override def getDefaultRep = Bind(element[Free[F, S]].defaultRepValue, constFun[S, Free[F, B]](element[Free[F, B]].defaultRepValue))
    override lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagB = eA.tag
      weakTypeTag[Bind[F, S, B]]
    }
  }

  // state representation type
  type BindData[F[_], S, B] = (Free[F, S], S => Free[F, B])

  // 3) Iso for concrete class
  class BindIso[F[_], S, B](implicit eS: Elem[S], eA: Elem[B], cF: Cont[F])
    extends EntityIso[BindData[F, S, B], Bind[F, S, B]] with Def[BindIso[F, S, B]] {
    override def from(p: Rep[Bind[F, S, B]]) =
      (p.a, p.f)
    override def to(p: Rep[(Free[F, S], S => Free[F, B])]) = {
      val Pair(a, f) = p
      Bind(a, f)
    }
    lazy val eFrom = pairElement(element[Free[F, S]], element[S => Free[F, B]])
    lazy val eTo = new BindElem[F, S, B](self)
    lazy val selfType = new BindIsoElem[F, S, B](eS, eA, cF)
    def productArity = 3
    def productElement(n: Int) = n match {
      case 0 => eS
      case 1 => eA
      case 2 => cF
    }
  }
  case class BindIsoElem[F[_], S, B](eS: Elem[S], eA: Elem[B], cF: Cont[F]) extends Elem[BindIso[F, S, B]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new BindIso[F, S, B]()(eS, eA, cF))
    lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagB = eA.tag
      weakTypeTag[BindIso[F, S, B]]
    }
    lazy val typeArgs = TypeArgs("F" -> (cF -> scalan.util.Invariant), "S" -> (eS -> scalan.util.Invariant), "B" -> (eA -> scalan.util.Invariant))
  }
  // 4) constructor and deconstructor
  class BindCompanionAbs extends CompanionDef[BindCompanionAbs] with BindCompanion {
    def selfType = BindCompanionElem
    override def toString = "Bind"
    @scalan.OverloadId("fromData")
    def apply[F[_], S, B](p: Rep[BindData[F, S, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Rep[Bind[F, S, B]] =
      isoBind(eS, eA, cF).to(p)
    @scalan.OverloadId("fromFields")
    def apply[F[_], S, B](a: Rep[Free[F, S]], f: Rep[S => Free[F, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Rep[Bind[F, S, B]] =
      mkBind(a, f)

    def unapply[F[_], S, B](p: Rep[Free[F, B]]) = unmkBind(p)
  }
  lazy val BindRep: Rep[BindCompanionAbs] = new BindCompanionAbs
  lazy val Bind: BindCompanionAbs = proxyBindCompanion(BindRep)
  implicit def proxyBindCompanion(p: Rep[BindCompanionAbs]): BindCompanionAbs = {
    proxyOps[BindCompanionAbs](p)
  }

  implicit case object BindCompanionElem extends CompanionElem[BindCompanionAbs] {
    lazy val tag = weakTypeTag[BindCompanionAbs]
    protected def getDefaultRep = Bind
  }

  implicit def proxyBind[F[_], S, B](p: Rep[Bind[F, S, B]]): Bind[F, S, B] =
    proxyOps[Bind[F, S, B]](p)

  implicit class ExtendedBind[F[_], S, B](p: Rep[Bind[F, S, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]) {
    def toData: Rep[BindData[F, S, B]] = isoBind(eS, eA, cF).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoBind[F[_], S, B](implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Iso[BindData[F, S, B], Bind[F, S, B]] =
    reifyObject(new BindIso[F, S, B]()(eS, eA, cF))

  // 6) smart constructor and deconstructor
  def mkBind[F[_], S, B](a: Rep[Free[F, S]], f: Rep[S => Free[F, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Rep[Bind[F, S, B]]
  def unmkBind[F[_], S, B](p: Rep[Free[F, B]]): Option[(Rep[Free[F, S]], Rep[S => Free[F, B]])]

  registerModule(Frees_Module)
}

// Std -----------------------------------
trait FreesStd extends scalan.ScalanDslStd with FreesDsl {
  self: MonadsDslStd =>

  lazy val Free: Rep[FreeCompanionAbs] = new FreeCompanionAbs {
  }

  case class StdReturn[F[_], A]
      (override val a: Rep[A])(implicit eA: Elem[A], cF: Cont[F])
    extends AbsReturn[F, A](a) {
  }

  def mkReturn[F[_], A]
    (a: Rep[A])(implicit eA: Elem[A], cF: Cont[F]): Rep[Return[F, A]] =
    new StdReturn[F, A](a)
  def unmkReturn[F[_], A](p: Rep[Free[F, A]]) = p match {
    case p: Return[F, A] @unchecked =>
      Some((p.a))
    case _ => None
  }

  case class StdSuspend[F[_], A]
      (override val a: Rep[F[A]])(implicit eA: Elem[A], cF: Cont[F])
    extends AbsSuspend[F, A](a) {
  }

  def mkSuspend[F[_], A]
    (a: Rep[F[A]])(implicit eA: Elem[A], cF: Cont[F]): Rep[Suspend[F, A]] =
    new StdSuspend[F, A](a)
  def unmkSuspend[F[_], A](p: Rep[Free[F, A]]) = p match {
    case p: Suspend[F, A] @unchecked =>
      Some((p.a))
    case _ => None
  }

  case class StdBind[F[_], S, B]
      (override val a: Rep[Free[F, S]], override val f: Rep[S => Free[F, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F])
    extends AbsBind[F, S, B](a, f) {
  }

  def mkBind[F[_], S, B]
    (a: Rep[Free[F, S]], f: Rep[S => Free[F, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Rep[Bind[F, S, B]] =
    new StdBind[F, S, B](a, f)
  def unmkBind[F[_], S, B](p: Rep[Free[F, B]]) = p match {
    case p: Bind[F, S, B] @unchecked =>
      Some((p.a, p.f))
    case _ => None
  }
}

// Exp -----------------------------------
trait FreesExp extends scalan.ScalanDslExp with FreesDsl {
  self: MonadsDslExp =>

  lazy val Free: Rep[FreeCompanionAbs] = new FreeCompanionAbs {
  }

  case class ExpReturn[F[_], A]
      (override val a: Rep[A])(implicit eA: Elem[A], cF: Cont[F])
    extends AbsReturn[F, A](a)

  object ReturnMethods {
    // WARNING: Cannot generate matcher for method `flatMap`: Method has function arguments f

    object flatMapBy {
      def unapply(d: Def[_]): Option[(Rep[Return[F, A]], Rep[A => Free[F, B]]) forSome {type F[_]; type A; type B}] = d match {
        case MethodCall(receiver, method, Seq(f, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: ReturnElem[_, _] => true; case _ => false }) && method.getName == "flatMapBy" =>
          Some((receiver, f)).asInstanceOf[Option[(Rep[Return[F, A]], Rep[A => Free[F, B]]) forSome {type F[_]; type A; type B}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Return[F, A]], Rep[A => Free[F, B]]) forSome {type F[_]; type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object foldMap {
      def unapply(d: Def[_]): Option[(Rep[Return[F, A]], $tilde$greater[F, G]) forSome {type F[_]; type A; type G[_]}] = d match {
        case MethodCall(receiver, method, Seq(f, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: ReturnElem[_, _] => true; case _ => false }) && method.getName == "foldMap" =>
          Some((receiver, f)).asInstanceOf[Option[(Rep[Return[F, A]], $tilde$greater[F, G]) forSome {type F[_]; type A; type G[_]}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Return[F, A]], $tilde$greater[F, G]) forSome {type F[_]; type A; type G[_]}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object resume {
      def unapply(d: Def[_]): Option[(Rep[Return[F, A]], Functor[F]) forSome {type F[_]; type A}] = d match {
        case MethodCall(receiver, method, Seq(fF, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: ReturnElem[_, _] => true; case _ => false }) && method.getName == "resume" =>
          Some((receiver, fF)).asInstanceOf[Option[(Rep[Return[F, A]], Functor[F]) forSome {type F[_]; type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Return[F, A]], Functor[F]) forSome {type F[_]; type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object ReturnCompanionMethods {
  }

  def mkReturn[F[_], A]
    (a: Rep[A])(implicit eA: Elem[A], cF: Cont[F]): Rep[Return[F, A]] =
    new ExpReturn[F, A](a)
  def unmkReturn[F[_], A](p: Rep[Free[F, A]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: ReturnElem[F, A] @unchecked =>
      Some((p.asRep[Return[F, A]].a))
    case _ =>
      None
  }

  case class ExpSuspend[F[_], A]
      (override val a: Rep[F[A]])(implicit eA: Elem[A], cF: Cont[F])
    extends AbsSuspend[F, A](a)

  object SuspendMethods {
    object foldMap {
      def unapply(d: Def[_]): Option[(Rep[Suspend[F, A]], $tilde$greater[F, G]) forSome {type F[_]; type A; type G[_]}] = d match {
        case MethodCall(receiver, method, Seq(trans, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: SuspendElem[_, _] => true; case _ => false }) && method.getName == "foldMap" =>
          Some((receiver, trans)).asInstanceOf[Option[(Rep[Suspend[F, A]], $tilde$greater[F, G]) forSome {type F[_]; type A; type G[_]}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Suspend[F, A]], $tilde$greater[F, G]) forSome {type F[_]; type A; type G[_]}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object resume {
      def unapply(d: Def[_]): Option[(Rep[Suspend[F, A]], Functor[F]) forSome {type F[_]; type A}] = d match {
        case MethodCall(receiver, method, Seq(fF, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: SuspendElem[_, _] => true; case _ => false }) && method.getName == "resume" =>
          Some((receiver, fF)).asInstanceOf[Option[(Rep[Suspend[F, A]], Functor[F]) forSome {type F[_]; type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Suspend[F, A]], Functor[F]) forSome {type F[_]; type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object SuspendCompanionMethods {
  }

  def mkSuspend[F[_], A]
    (a: Rep[F[A]])(implicit eA: Elem[A], cF: Cont[F]): Rep[Suspend[F, A]] =
    new ExpSuspend[F, A](a)
  def unmkSuspend[F[_], A](p: Rep[Free[F, A]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: SuspendElem[F, A] @unchecked =>
      Some((p.asRep[Suspend[F, A]].a))
    case _ =>
      None
  }

  case class ExpBind[F[_], S, B]
      (override val a: Rep[Free[F, S]], override val f: Rep[S => Free[F, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F])
    extends AbsBind[F, S, B](a, f)

  object BindMethods {
    // WARNING: Cannot generate matcher for method `flatMap`: Method has function arguments f1

    object flatMapBy {
      def unapply(d: Def[_]): Option[(Rep[Bind[F, S, B]], Rep[B => Free[F, R]]) forSome {type F[_]; type S; type B; type R}] = d match {
        case MethodCall(receiver, method, Seq(f1, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: BindElem[_, _, _] => true; case _ => false }) && method.getName == "flatMapBy" =>
          Some((receiver, f1)).asInstanceOf[Option[(Rep[Bind[F, S, B]], Rep[B => Free[F, R]]) forSome {type F[_]; type S; type B; type R}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Bind[F, S, B]], Rep[B => Free[F, R]]) forSome {type F[_]; type S; type B; type R}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object foldMap {
      def unapply(d: Def[_]): Option[(Rep[Bind[F, S, B]], $tilde$greater[F, G]) forSome {type F[_]; type S; type B; type G[_]}] = d match {
        case MethodCall(receiver, method, Seq(trans, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: BindElem[_, _, _] => true; case _ => false }) && method.getName == "foldMap" =>
          Some((receiver, trans)).asInstanceOf[Option[(Rep[Bind[F, S, B]], $tilde$greater[F, G]) forSome {type F[_]; type S; type B; type G[_]}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Bind[F, S, B]], $tilde$greater[F, G]) forSome {type F[_]; type S; type B; type G[_]}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object resume {
      def unapply(d: Def[_]): Option[(Rep[Bind[F, S, B]], Functor[F]) forSome {type F[_]; type S; type B}] = d match {
        case MethodCall(receiver, method, Seq(fF, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: BindElem[_, _, _] => true; case _ => false }) && method.getName == "resume" =>
          Some((receiver, fF)).asInstanceOf[Option[(Rep[Bind[F, S, B]], Functor[F]) forSome {type F[_]; type S; type B}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Bind[F, S, B]], Functor[F]) forSome {type F[_]; type S; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object BindCompanionMethods {
  }

  def mkBind[F[_], S, B]
    (a: Rep[Free[F, S]], f: Rep[S => Free[F, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Rep[Bind[F, S, B]] =
    new ExpBind[F, S, B](a, f)
  def unmkBind[F[_], S, B](p: Rep[Free[F, B]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: BindElem[F, S, B] @unchecked =>
      Some((p.asRep[Bind[F, S, B]].a, p.asRep[Bind[F, S, B]].f))
    case _ =>
      None
  }

  object FreeMethods {
    // WARNING: Cannot generate matcher for method `flatMap`: Method has function arguments f

    object flatMapBy {
      def unapply(d: Def[_]): Option[(Rep[Free[F, A]], Rep[A => Free[F, B]]) forSome {type F[_]; type A; type B}] = d match {
        case MethodCall(receiver, method, Seq(f, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: FreeElem[_, _, _] => true; case _ => false }) && method.getName == "flatMapBy" =>
          Some((receiver, f)).asInstanceOf[Option[(Rep[Free[F, A]], Rep[A => Free[F, B]]) forSome {type F[_]; type A; type B}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Free[F, A]], Rep[A => Free[F, B]]) forSome {type F[_]; type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object mapBy {
      def unapply(d: Def[_]): Option[(Rep[Free[F, A]], Rep[A => B]) forSome {type F[_]; type A; type B}] = d match {
        case MethodCall(receiver, method, Seq(f, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: FreeElem[_, _, _] => true; case _ => false }) && method.getName == "mapBy" =>
          Some((receiver, f)).asInstanceOf[Option[(Rep[Free[F, A]], Rep[A => B]) forSome {type F[_]; type A; type B}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Free[F, A]], Rep[A => B]) forSome {type F[_]; type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    // WARNING: Cannot generate matcher for method `map`: Method has function arguments f

    object foldMap {
      def unapply(d: Def[_]): Option[(Rep[Free[F, A]], $tilde$greater[F, G]) forSome {type F[_]; type A; type G[_]}] = d match {
        case MethodCall(receiver, method, Seq(f, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: FreeElem[_, _, _] => true; case _ => false }) && method.getName == "foldMap" =>
          Some((receiver, f)).asInstanceOf[Option[(Rep[Free[F, A]], $tilde$greater[F, G]) forSome {type F[_]; type A; type G[_]}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Free[F, A]], $tilde$greater[F, G]) forSome {type F[_]; type A; type G[_]}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object run {
      def unapply(d: Def[_]): Option[(Rep[Free[F, A]], $tilde$greater[F, G]) forSome {type F[_]; type A; type G[_]}] = d match {
        case MethodCall(receiver, method, Seq(f, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: FreeElem[_, _, _] => true; case _ => false }) && method.getName == "run" =>
          Some((receiver, f)).asInstanceOf[Option[(Rep[Free[F, A]], $tilde$greater[F, G]) forSome {type F[_]; type A; type G[_]}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Free[F, A]], $tilde$greater[F, G]) forSome {type F[_]; type A; type G[_]}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object step {
      def unapply(d: Def[_]): Option[Rep[Free[F, A]] forSome {type F[_]; type A}] = d match {
        case MethodCall(receiver, method, _, _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: FreeElem[_, _, _] => true; case _ => false }) && method.getName == "step" =>
          Some(receiver).asInstanceOf[Option[Rep[Free[F, A]] forSome {type F[_]; type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[Free[F, A]] forSome {type F[_]; type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object resume {
      def unapply(d: Def[_]): Option[(Rep[Free[F, A]], Functor[F]) forSome {type F[_]; type A}] = d match {
        case MethodCall(receiver, method, Seq(fF, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: FreeElem[_, _, _] => true; case _ => false }) && method.getName == "resume" =>
          Some((receiver, fF)).asInstanceOf[Option[(Rep[Free[F, A]], Functor[F]) forSome {type F[_]; type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Free[F, A]], Functor[F]) forSome {type F[_]; type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object FreeCompanionMethods {
  }
}

object Frees_Module extends scalan.ModuleInfo {
  val dump = "H4sIAAAAAAAAANVXS2wbRRgeP2LHcWhCK9pCFRIiN1AedkUPRYpQFSd2SOU8lC0U0qrReHecbtmdXXbH6ZpDuVUCbgghgcShCMQlQkJcEGeQqqrqAXHjBBJcWirUA5WQQPwz+7TjdR6lVPgw2hn/+z++7/tnZzZ+Q322hSZsGWuYFnXCcFESz1M2K0gVylTWmjeUpkZmSOPcS5/8eUZ/+0ASDa+gzHlsz9jaCsq5DxXHDJ4lptRQDlOZ2MywbIaeqIkIJdnQNCIz1aAlVdebDNc1UqqpNpusoXTdUFpvoEsoUUPDskFlizAiTWvYtontrfcTnpEazHNi3lo0wxi0xKsoRao4ZWGVQfoQY9i1Xyam1KIGbekM7fFSWzR5WmCTJ44JNczppibCpGooq+qmYTE/ahYinDcUf5qmGBbQ3toFvI5LEHWtJDFLpWvcmYnl1/EaWQATbp6GGmyiNU61TOI5z9tMaYvnmAghYOV5kVgxxKwYYFbkmBUkYqlYU9/E/M8ly3BayP0lUgg5Jrh4dgsXvgdSoUrhnbPymbtSXk/ylx2eSlYklAFHozEKEfQAtleX37PvzF45nkQDK2hAtafqNrOwzKIy8ODKY0oNJnIOEMTWGjA4HsegiDIFNh0yycmGbmIKnjwsB4EoTZVVxo352qBHTwz2WWYS3zThmImg3rGYeoWWprGmLd189LnDtyqvJlGyPUQOXErQDJbvlKF01SLEc83HIYYS1RBfPp0SUz7knHDM9sgkwOTJm7eV746is8kASS/w9sgDF3tf+Oibw2TpiyTqXxFar2p4TdDIoZohtryC+o11Yrnr2XWs8aeuVGYV0sBNjXkAR5FJATIMjcW2qUk4bJNC/gm//Lyr4AWDkkJ1qfCHdO39DS5QCw26/7h9+7d6/K8f9zSY0C7giX1kU9DrHdDHYz3gupQMnTw8fkc9d+VdJlBNOO3tvVi/AO00Kd57vAfA/s7z5eXLj/z+6eo+0R39dZXp2Cwc3UFv+FK+j9pHASquKg+Ecz6MAqpDy4Q1LTodDTwaeSOC82MJn0NhxFCSTPkEpCsa0XtwEuNArgYOpg3KuvZTlFSGMm6+wkHQDCNxXAk0XrulFA/eHrmYRJmTqK8BKrdrqK9uNKniwwyfJkYcVvbXEu0wA6zYwnrwxVrHsMUCzQzt95XfZKpWesVbd/UOvzEU4skLihR4oqvFqi/b/V5J3G9xjroRWeGZrzcuqjeOVEVPhMjM9nAZ0jAb4SKfaAf7HvazTTpBHTT37NxqD9V0jzUhxiPbEPew1LRNQpX/jbqzXsJRecczu4Wc+FDpJP8/U0v5fqhlG3GlTXFjwjS6hLFg64/dSqpNKv8w9+G+oZHVn8QRIaMYOlaFqA7BjmLBZ0TsGIe873SYzj3jtjv9P1RWdy1+qZf4oyDvqnvKWzvYefekebkPrnVCUHpZlbu59UDIeMy215qC08a/tFm3NWGHfnYgKx6+i6z8Y/GDo2DT7hWOV8OsJqDPizF9PkNkDVtE4Vc3osPV0j24HfvgxOmTB0+/LI6Og4owcv8JTr/dL8Lz2JwU17anelzbwKhQ0U24lsPDsW9f/P6t659/Fnzis151fYJ1gN9P3aBYsYOKxmMqkrwjIqjo0t2PF56+8dUvYvca4IdNOILT4A4cHnmcjn03Ny9iwZU2Iitgmx8/I+L5mQ+//gPB+XhfgRAAAA=="
}
}

