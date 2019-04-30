package scalan.primitives

import scalan.{Base, Scalan}

trait LogicalOps extends Base { self: Scalan =>
  val And = new EndoBinOp[Boolean]("&&", _ && _)

  val Or = new EndoBinOp[Boolean]("||", _ || _)

  val Not = new EndoUnOp[Boolean]("!", !_)

  val BinaryXorOp = new EndoBinOp[Boolean]("^", _ ^ _)

  val BooleanToInt = new UnOp[Boolean, Int]("ToInt", if (_) 1 else 0)

  implicit class RepBooleanOps(value: Rep[Boolean]) {
    def &&(y: Rep[Boolean]): Rep[Boolean] = And(value, y)
    def ||(y: Rep[Boolean]): Rep[Boolean] = Or(value, y)
    def ^(y: Rep[Boolean]): Rep[Boolean] = BinaryXorOp(value, y)

    def lazy_&&(y: Rep[Thunk[Boolean]]): Rep[Boolean] = And.applyLazy(value, y)
    def lazy_||(y: Rep[Thunk[Boolean]]): Rep[Boolean] = Or.applyLazy(value, y)

    def unary_!() : Rep[Boolean] = Not(value)
    def toInt: Rep[Int] = BooleanToInt(value)
  }

  implicit class BooleanFuncOps[A](f: Rep[A => Boolean]) {
    def &&&(g: Rep[A => Boolean]) =
      if (f == g)
        f
      else
        composeBi(f, g)(_ && _)

    def |||(g: Rep[A => Boolean]) =
      if (f == g)
        f
      else
        composeBi(f, g)(_ || _)

    def !!! = sameArgFun(f) { x => !f(x) }
  }

  override def rewriteDef[A](d: Def[A]) = d match {
    case ApplyBinOp(op, lhs, rhs) =>
      op.asInstanceOf[BinOp[_, _]] match {
        case _: Equals[_] if lhs.elem == BooleanElement && rhs.elem == BooleanElement =>
          matchBoolConsts(d, lhs, rhs, x => x, x => !x.asInstanceOf[Rep[Boolean]], _ => true, _ => false)
        case And =>
          matchBoolConsts(d, lhs, rhs, x => x, _ => false, x => x, _ => false)
        case Or =>
          matchBoolConsts(d, lhs, rhs, _ => true, x => x, x => x, _ => true)
        case BinaryXorOp =>
          matchBoolConsts(d, lhs, rhs, x => !x.asInstanceOf[Rep[Boolean]], x => x.asInstanceOf[Rep[Boolean]], _ => false, _ => true)
        case _ => super.rewriteDef(d)
      }
    case ApplyUnOp(o1, Def(ApplyUnOp(o2, x))) if o1 == Not && o2 == Not => x
    case _ => super.rewriteDef(d)
  }

  @inline
  private def matchBoolConsts(d: Def[_], lhs: Sym, rhs: Sym, ifTrue: Sym => Sym, ifFalse: Sym => Sym, ifEqual: Sym => Sym, ifNegated: Sym => Sym): Sym =
    lhs match {
      case `rhs` =>
        ifEqual(lhs)
      case Def(ApplyUnOp(op, `rhs`)) if op == Not =>
        ifNegated(lhs)
      case Def(Const(b: Boolean)) =>
        if (b) ifTrue(rhs) else ifFalse(rhs)
      case _ => rhs match {
        case Def(Const(b: Boolean)) =>
          if (b) ifTrue(lhs) else ifFalse(lhs)
        case Def(ApplyUnOp(op, `lhs`)) if op == Not =>
          ifNegated(rhs)
        case _ => super.rewriteDef(d)
      }
    }
}
