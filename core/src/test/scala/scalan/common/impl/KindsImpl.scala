package scalan.common

import scalan.{Base, Scalan}
import scala.reflect.runtime.universe.{WeakTypeTag, weakTypeTag}
import scalan.meta.ScalanAst._

package impl {
// Abs -----------------------------------
trait KindsDefs extends scalan.Scalan with Kinds {
  self: KindsModule =>

  // entityProxy: single proxy for each type family
  implicit def proxyKind[F[_], A](p: Rep[Kind[F, A]]): Kind[F, A] = {
    proxyOps[Kind[F, A]](p)(scala.reflect.classTag[Kind[F, A]])
  }

  // familyElem
  class KindElem[F[_], A, To <: Kind[F, A]](implicit _cF: Cont[F], _eA: Elem[A])
    extends EntityElem[To] {
    def cF = _cF
    def eA = _eA
    lazy val parent: Option[Elem[_]] = None
    lazy val typeArgs = TypeArgs("F" -> (cF -> scalan.util.Invariant), "A" -> (eA -> scalan.util.Invariant))
    override lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[Kind[F, A]].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      val conv = fun {x: Rep[Kind[F, A]] => convertKind(x) }
      tryConvert(element[Kind[F, A]], this, x, conv)
    }

    def convertKind(x: Rep[Kind[F, A]]): Rep[To] = {
      x.elem.asInstanceOf[Elem[_]] match {
        case _: KindElem[_, _, _] => x.asRep[To]
        case e => !!!(s"Expected $x to have KindElem[_, _, _], but got $e", x)
      }
    }

    override def getDefaultRep: Rep[To] = ???
  }

  implicit def kindElement[F[_], A](implicit cF: Cont[F], eA: Elem[A]): Elem[Kind[F, A]] =
    cachedElem[KindElem[F, A, Kind[F, A]]](cF, eA)

  implicit case object KindCompanionElem extends CompanionElem[KindCompanionCtor] {
    lazy val tag = weakTypeTag[KindCompanionCtor]
    protected def getDefaultRep = Kind
  }

  abstract class KindCompanionCtor extends CompanionDef[KindCompanionCtor] with KindCompanion {
    def selfType = KindCompanionElem
    override def toString = "Kind"
  }
  implicit def proxyKindCompanionCtor(p: Rep[KindCompanionCtor]): KindCompanionCtor =
    proxyOps[KindCompanionCtor](p)

  case class ReturnCtor[F[_], A]
      (override val a: Rep[A])(implicit cF: Cont[F])
    extends Return[F, A](a) with Def[Return[F, A]] {
    implicit val eA = a.elem
    lazy val selfType = element[Return[F, A]]
  }
  // elem for concrete class
  class ReturnElem[F[_], A](val iso: Iso[ReturnData[F, A], Return[F, A]])(implicit override val eA: Elem[A], override val cF: Cont[F])
    extends KindElem[F, A, Return[F, A]]
    with ConcreteElem[ReturnData[F, A], Return[F, A]] {
    override lazy val parent: Option[Elem[_]] = Some(kindElement(container[F], element[A]))
    override lazy val typeArgs = TypeArgs("F" -> (cF -> scalan.util.Invariant), "A" -> (eA -> scalan.util.Invariant))

    override def convertKind(x: Rep[Kind[F, A]]) = // Converter is not generated by meta
!!!("Cannot convert from Kind to Return: missing fields List(a)")
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
    def getDefaultRep = reifyObject(new ReturnIso[F, A]()(eA, cF))
    lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[ReturnIso[F, A]]
    }
    lazy val typeArgs = TypeArgs("F" -> (cF -> scalan.util.Invariant), "A" -> (eA -> scalan.util.Invariant))
  }
  // 4) constructor and deconstructor
  class ReturnCompanionCtor extends CompanionDef[ReturnCompanionCtor] with ReturnCompanion {
    def selfType = ReturnCompanionElem
    override def toString = "ReturnCompanion"

    @scalan.OverloadId("fromFields")
    def apply[F[_], A](a: Rep[A])(implicit cF: Cont[F]): Rep[Return[F, A]] =
      mkReturn(a)

    def unapply[F[_], A](p: Rep[Kind[F, A]]) = unmkReturn(p)
  }
  lazy val ReturnRep: Rep[ReturnCompanionCtor] = new ReturnCompanionCtor
  lazy val Return: ReturnCompanionCtor = proxyReturnCompanion(ReturnRep)
  implicit def proxyReturnCompanion(p: Rep[ReturnCompanionCtor]): ReturnCompanionCtor = {
    proxyOps[ReturnCompanionCtor](p)
  }

  implicit case object ReturnCompanionElem extends CompanionElem[ReturnCompanionCtor] {
    lazy val tag = weakTypeTag[ReturnCompanionCtor]
    protected def getDefaultRep = ReturnRep
  }

  implicit def proxyReturn[F[_], A](p: Rep[Return[F, A]]): Return[F, A] =
    proxyOps[Return[F, A]](p)

  implicit class ExtendedReturn[F[_], A](p: Rep[Return[F, A]])(implicit cF: Cont[F]) {
    def toData: Rep[ReturnData[F, A]] = {
      implicit val eA = p.a.elem
      isoReturn(eA, cF).from(p)
    }
  }

  // 5) implicit resolution of Iso
  implicit def isoReturn[F[_], A](implicit eA: Elem[A], cF: Cont[F]): Iso[ReturnData[F, A], Return[F, A]] =
    reifyObject(new ReturnIso[F, A]()(eA, cF))

  case class BindCtor[F[_], S, B]
      (override val a: Rep[Kind[F, S]], override val f: Rep[S => Kind[F, B]])
    extends Bind[F, S, B](a, f) with Def[Bind[F, S, B]] {
    implicit val cF = a.elem.typeArgs("F")._1.asCont[F];
implicit val eS = a.elem.typeArgs("A")._1.asElem[S];
implicit val eA = f.elem.eRange.typeArgs("A")._1.asElem[B]
    lazy val selfType = element[Bind[F, S, B]]
  }
  // elem for concrete class
  class BindElem[F[_], S, B](val iso: Iso[BindData[F, S, B], Bind[F, S, B]])(implicit val eS: Elem[S], override val eA: Elem[B], override val cF: Cont[F])
    extends KindElem[F, B, Bind[F, S, B]]
    with ConcreteElem[BindData[F, S, B], Bind[F, S, B]] {
    override lazy val parent: Option[Elem[_]] = Some(kindElement(container[F], element[B]))
    override lazy val typeArgs = TypeArgs("F" -> (cF -> scalan.util.Invariant), "S" -> (eS -> scalan.util.Invariant), "B" -> (eA -> scalan.util.Invariant))

    override def convertKind(x: Rep[Kind[F, B]]) = // Converter is not generated by meta
!!!("Cannot convert from Kind to Bind: missing fields List(a, f)")
    override def getDefaultRep = Bind(element[Kind[F, S]].defaultRepValue, constFun[S, Kind[F, B]](element[Kind[F, B]].defaultRepValue))
    override lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagB = eA.tag
      weakTypeTag[Bind[F, S, B]]
    }
  }

  // state representation type
  type BindData[F[_], S, B] = (Kind[F, S], S => Kind[F, B])

  // 3) Iso for concrete class
  class BindIso[F[_], S, B](implicit eS: Elem[S], eA: Elem[B], cF: Cont[F])
    extends EntityIso[BindData[F, S, B], Bind[F, S, B]] with Def[BindIso[F, S, B]] {
    override def from(p: Rep[Bind[F, S, B]]) =
      (p.a, p.f)
    override def to(p: Rep[(Kind[F, S], S => Kind[F, B])]) = {
      val Pair(a, f) = p
      Bind(a, f)
    }
    lazy val eFrom = pairElement(element[Kind[F, S]], element[S => Kind[F, B]])
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
    def getDefaultRep = reifyObject(new BindIso[F, S, B]()(eS, eA, cF))
    lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagB = eA.tag
      weakTypeTag[BindIso[F, S, B]]
    }
    lazy val typeArgs = TypeArgs("F" -> (cF -> scalan.util.Invariant), "S" -> (eS -> scalan.util.Invariant), "B" -> (eA -> scalan.util.Invariant))
  }
  // 4) constructor and deconstructor
  class BindCompanionCtor extends CompanionDef[BindCompanionCtor] with BindCompanion {
    def selfType = BindCompanionElem
    override def toString = "BindCompanion"
    @scalan.OverloadId("fromData")
    def apply[F[_], S, B](p: Rep[BindData[F, S, B]]): Rep[Bind[F, S, B]] = {
      implicit val cF = p._1.elem.typeArgs("F")._1.asCont[F];
implicit val eS = p._1.elem.typeArgs("A")._1.asElem[S];
implicit val eB = p._2.elem.eRange.typeArgs("A")._1.asElem[B]
      isoBind[F, S, B].to(p)
    }

    @scalan.OverloadId("fromFields")
    def apply[F[_], S, B](a: Rep[Kind[F, S]], f: Rep[S => Kind[F, B]]): Rep[Bind[F, S, B]] =
      mkBind(a, f)

    def unapply[F[_], S, B](p: Rep[Kind[F, B]]) = unmkBind(p)
  }
  lazy val BindRep: Rep[BindCompanionCtor] = new BindCompanionCtor
  lazy val Bind: BindCompanionCtor = proxyBindCompanion(BindRep)
  implicit def proxyBindCompanion(p: Rep[BindCompanionCtor]): BindCompanionCtor = {
    proxyOps[BindCompanionCtor](p)
  }

  implicit case object BindCompanionElem extends CompanionElem[BindCompanionCtor] {
    lazy val tag = weakTypeTag[BindCompanionCtor]
    protected def getDefaultRep = BindRep
  }

  implicit def proxyBind[F[_], S, B](p: Rep[Bind[F, S, B]]): Bind[F, S, B] =
    proxyOps[Bind[F, S, B]](p)

  implicit class ExtendedBind[F[_], S, B](p: Rep[Bind[F, S, B]]) {
    def toData: Rep[BindData[F, S, B]] = {
      implicit val cF = p.a.elem.typeArgs("F")._1.asCont[F];
implicit val eS = p.a.elem.typeArgs("A")._1.asElem[S];
implicit val eB = p.f.elem.eRange.typeArgs("A")._1.asElem[B]
      isoBind(eS, eB, cF).from(p)
    }
  }

  // 5) implicit resolution of Iso
  implicit def isoBind[F[_], S, B](implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Iso[BindData[F, S, B], Bind[F, S, B]] =
    reifyObject(new BindIso[F, S, B]()(eS, eA, cF))

  registerModule(KindsModule)

  lazy val Kind: Rep[KindCompanionCtor] = new KindCompanionCtor {
  }

  object ReturnMethods {
    // WARNING: Cannot generate matcher for method `flatMap`: Method has function arguments f
  }

  object ReturnCompanionMethods {
  }

  def mkReturn[F[_], A]
    (a: Rep[A])(implicit cF: Cont[F]): Rep[Return[F, A]] = {
    new ReturnCtor[F, A](a)
  }
  def unmkReturn[F[_], A](p: Rep[Kind[F, A]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: ReturnElem[F, A] @unchecked =>
      Some((p.asRep[Return[F, A]].a))
    case _ =>
      None
  }

  object BindMethods {
    // WARNING: Cannot generate matcher for method `flatMap`: Method has function arguments f1
  }

  object BindCompanionMethods {
  }

  def mkBind[F[_], S, B]
    (a: Rep[Kind[F, S]], f: Rep[S => Kind[F, B]]): Rep[Bind[F, S, B]] = {
    new BindCtor[F, S, B](a, f)
  }
  def unmkBind[F[_], S, B](p: Rep[Kind[F, B]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: BindElem[F, S, B] @unchecked =>
      Some((p.asRep[Bind[F, S, B]].a, p.asRep[Bind[F, S, B]].f))
    case _ =>
      None
  }

  object KindMethods {
    // WARNING: Cannot generate matcher for method `flatMap`: Method has function arguments f

    object mapBy {
      def unapply(d: Def[_]): Option[(Rep[Kind[F, A]], Rep[A => B]) forSome {type F[_]; type A; type B}] = d match {
        case MethodCall(receiver, method, Seq(f, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: KindElem[_, _, _] => true; case _ => false }) && method.getName == "mapBy" =>
          Some((receiver, f)).asInstanceOf[Option[(Rep[Kind[F, A]], Rep[A => B]) forSome {type F[_]; type A; type B}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Kind[F, A]], Rep[A => B]) forSome {type F[_]; type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object KindCompanionMethods {
  }
}

object KindsModule extends scalan.ModuleInfo {
  val dump = "H4sIAAAAAAAAAM1XX4gbRRif7OUud7l/7dWrWK1ezxzWKsnpS4V7kOSaSG28O25rhbN4TDaTOO3+c3dybqT0sQ/2rfiiIFgoCHIo0hcRKaII4kOffBGfVAqCKNIHi4LFb2Z3NpvcbnJXEczDsDOZ+f78fr/vm93tX9Gw66A5V8M6NvMGYTiviueiy3LqC1a9pZMTpPGjdX3myuU/P1DQvg00Sd0z1GEtrNM3SH0DzVrnywZlqw5t+gdOO5iyKposm4yyds4Qiwwdq/puCtxNIc5NLjixVEUzp9s2UdumZVIjtFAYbCF6DMzc95KDbZs4PaE8NdhQ90EwNYZNjbjMclyGjvjnC5ql60Rj1DIL1DBaDNd0UqhSl8H+fZplag5hRF3WsesS9zV0EaWraJRwkzScj4l5e9Xu2N0Zl4AUwuJ2/f3rxBZ5tg2GpoJwVm0eCuzJUMO2HCZdZMDcq1ZdTtMmhgU0Uz2Ht3ABXDQLKnOo2YST01Y3jfzISBWN21g7j5tkBU7ypQzk4RK9weEWWzw7lbJtG8T0tIgl34EmH0KT59DkVOJQrh3M/1xzLK+N/F9qCCGPm3hygAlpgZTNeu7Ns9rLd9RxQ+GHPZHjGNh4JEHTggxA8uv1K+7t564eV1B2A2WpW6y5zMEaixId4DWOTdNiItwQQuw0ga/5JL6ElyLsAUjTNavelmRrlmFjEywFwE4AUzrVKOOb+dp0wE8sykAls4ncmgbQw3yTapifLdq23v7ywo0LPz303X4FDXERerYTMTsEZvukI6SwjHUd0lGYdA5esz5TqmWQ/fO36StXLzMFpaoo5XXra7V2Dphc8hw04Z/wpXqXHv/7+6kGUwLiE5OQ/j/LfP7Fz7eeTStI6cZpDBJQy5CUDI6h9Clq1gN8+PgAQ6kKf8iG06KY8mHC4+Ohnnm2T0whxY/98lv9q0V0ViQuhCHx2ZUWwcTMM+98ukDWPlTQ6Iao3YqOm0KVnKITxNU20Ki1RRx/PbOFdf4Uq8xMnTRwS5eFG8XIJ3kukWSbcACXPJvXokx/3OdrxTJJrrKW+0P95q1tThf//36AEEswh6Aj9aDdC68ElY+zfXCR3e7jS5dmf7+2eUDU6GiNMgPbucU9VKgsqP+wAlGYo5/Xkc6cD0dBiNPrhLUccznq+GjkRAS1XEpCLzYxpJCihDNd1onRB+EEA1olNLBsmSy2IKIUMTTixysMhBo+nMSVQOOjv0rvP3rowbsKyjyPhhsgTjeWleGa1TLrEnm4IRnxWEmupbuRB6Sxg43w4tzC0PaBeYYOSg23GNULZ4J1X7nwm+uQ4uPME40kXknetil1fzDIl3vInzR93yz3xCfbr9Obj1dEx+rAtjLIboeolQhbU6lIVDNBXUi2BrevUg95L+5oXzv0hXrk0a9+dxGBukM+CW4aMW4c9HCyqCotU/v25NsHpg9v/iC6/UjdMjAVqlwAITlwrQihLASNthPOv0Awrk/xcXEXZT5ZAnf3WORqvyKPgnxPXaI02MDeu0SapxvtEQMqYDcFyIdqb6Xsqr46GA3cWopzMOFFwltEyWU5BPfSnssk/iK8Fl+uPUrbgwBPxQtQvgv9b8ja0Qw743vd9mH3cAgv6HQyaBhwiRvBrcJfJOYT+ogaXN3A2cU7764cu3n9luglWf4SAG80ZviJ1Ll3vJ4uOC78+59EEcIAU/5i8A/c84tPQg8AAA=="
}
}

trait KindsModule extends scalan.common.impl.KindsDefs
