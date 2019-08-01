package scalan

trait DefRewriting { scalan: Scalan =>

  def rewriteDef[T](d: Def[T]): Rep[_] = d match {
    case First(p) if p.rhs.isInstanceOf[Tup[_,_]] => p.rhs.asInstanceOf[Tup[_,_]].a
    case Second(p) if p.rhs.isInstanceOf[Tup[_,_]] => p.rhs.asInstanceOf[Tup[_,_]].b
    case Tup(Def(First(a)), Def(Second(b))) if a == b => a

    // Rule: convert(eFrom, eTo, x, conv) if x.elem <:< eFrom  ==>  conv(x)
    case Convert(eFrom: Elem[from], eTo: Elem[to], x,  conv) if x.elem <:< eFrom =>
      mkApply(conv, x)

    case Apply(f @ Def(l: Lambda[a,b]), x, mayInline) if mayInline && l.mayInline =>
      mkApply(f, x)

    case call @ MethodCall(receiver, m, args, neverInvoke) =>
      call.tryInvoke match {
        // Rule: receiver.m(args) ==> body(m).subst{xs -> args}
        case InvokeSuccess(res) => res
        case InvokeFailure(e) if !e.isInstanceOf[DelayInvokeException] =>
          throwInvocationException("Method invocation in rewriteDef", e, receiver, m, args)
        case InvokeImpossible =>
          val res = rewriteNonInvokableMethodCall(call)
          if (res != null) res
          else
            null
      }

    case ThunkForce(th) =>
      th.rhs match {
        // empty Thunk
        case ThunkDef(root, sch) if sch.isEmpty => root
        // constant in Thunk
        case ConstantThunk(rootConst) => rootConst
        case _ => null
      }

    case ApplyUnOp(op, x) => op match {
      case _: NumericNegate[_] => x.rhs match {
        // -(-x) => x
        case ApplyUnOp(_: NumericNegate[_], x) => x
        case _ => null
      }
      // (x: Int).toInt => x
      case NumericToInt(_) if x.elem == IntElement => x
      // (x: Long).toLong => x
      case NumericToLong(_) if x.elem == LongElement => x
      // (x: Float).toFloat => x
      case NumericToFloat(_) if x.elem == FloatElement => x
      // (x: Double).toDouble => x
      case NumericToDouble(_) if x.elem == DoubleElement => x

      case _ if op == Not => x.rhs match {
        // Rule: !(x op y) ==>
        case ApplyBinOp(op, x, y) => op.asInstanceOf[BinOp[_,_]] match {
          case OrderingLT(ord) =>
            OrderingGTEQ(ord)(x, y)
          case OrderingLTEQ(ord) =>
            OrderingGT(ord)(x, y)
          case OrderingGT(ord) =>
            OrderingLTEQ(ord)(x, y)
          case OrderingGTEQ(ord) =>
            OrderingLT(ord)(x, y)
          case _ => null
        }
        // Rule: !(!(x)) ==> x
        case ApplyUnOp(op, x) if op == Not => x
        // Rule: !Const(x) => Const(!x)
        case Const(x: Boolean) if currentPass.config.constantPropagation => Const(!x)
        case _ => null
      }
      case _ => propagateUnOp(op, x)
    }

    case ApplyBinOp(op, x, y) => op.asInstanceOf[BinOp[_,_]] match {
      case _: Equals[_] =>
        if (x == y) Const(true)
        else {
          y match {
            case Def(Const(b: Boolean)) if x.elem == BooleanElement =>
              if (b) x else Not(asRep[Boolean](x))
            case _ =>
              x match {
                case Def(Const(b: Boolean)) if y.elem == BooleanElement =>
                  if (b) y else Not(asRep[Boolean](y))
                case _ =>
                  null
              }
          }
        }
      case _: NotEquals[_] =>
        if (x == y) Const(false)
        else {
          y match {
            case Def(Const(b: Boolean)) if x.elem == BooleanElement =>
              if (b) Not(asRep[Boolean](x)) else x
            case _ =>
              x match {
                case Def(Const(b: Boolean)) if y.elem == BooleanElement =>
                  if (b) Not(asRep[Boolean](y)) else y
                case _ =>
                  null
              }
          }
        }
      case And =>
        rewriteBoolConsts(d, x, y, x => x, _ => false, x => x, _ => false)
      case Or =>
        rewriteBoolConsts(d, x, y, _ => true, x => x, x => x, _ => true)
      case BinaryXorOp =>
        rewriteBoolConsts(d, x, y, x => !x.asInstanceOf[Rep[Boolean]], x => x.asInstanceOf[Rep[Boolean]], _ => false, _ => true)

      case NumericPlus(n: Numeric[a]) => (x, y) match {
        // x + 0 => x
        case (x, Def(Const(zero))) if isZero(zero.asInstanceOf[a], n) => x
        // 0 + x => x
        case (Def(Const(zero)), x) if isZero(zero.asInstanceOf[a], n) => x
        case _ => propagateBinOp(op, x, y)
      }

      case NumericMinus(n: Numeric[a]) => (x, y) match {
        // x - 0 => x
        case (x, Def(Const(zero))) if isZero(zero.asInstanceOf[a], n) => x
        // 0 - x => -x
        case (Def(Const(zero)), x) if isZero(zero.asInstanceOf[a], n) =>
          new NumericOpsCls(asRep[a](x))(n).unary_-
        case _ => propagateBinOp(op, x, y)
      }

      case NumericTimes(n: Numeric[a]) => (x, y) match {
        // _ * 0 => 0
        case (_, y@Def(Const(zero))) if isZero(zero.asInstanceOf[a], n) => y
        // 0 * _ => 0
        case (y@Def(Const(zero)), _) if isZero(zero.asInstanceOf[a], n) => y
        // x * 1 => x
        case (x, Def(Const(one))) if isOne(one.asInstanceOf[a], n) => x
        // 1 * x => x
        case (Def(Const(one)), x) if isOne(one.asInstanceOf[a], n) => x
        case _ => propagateBinOp(op, x, y)
      }

      case FractionalDivide(n: Fractional[a]) => (x, y) match {
        // 0 / _ => 0
        case (x@Def(Const(zero)), _) if isZero(zero.asInstanceOf[a], n) => x
        // x / 1 => x
        case (x, Def(Const(one))) if isOne(one.asInstanceOf[a], n) => x
        // 0 / _ => 0 (for ints)
        case (x@Def(Const(zero)), _) if isZero(zero.asInstanceOf[a], n) => x
        // x / 1 => x (for ints)
        case (x, Def(Const(one))) if isOne(one.asInstanceOf[a], n) => x
        case _ => propagateBinOp(op, x, y)
      }

      case _ => propagateBinOp(op, x, y)
    }

    case _ => null
  }

  def propagateUnOp[T,R](op: UnOp[T,R], x: Rep[T]): Rep[R] = {
    if (currentPass.config.constantPropagation) {
      if (x.isConst) {
        val xVal = valueFromRep(x)
        if (op.shouldPropagate(xVal))
          Const(op.applySeq(xVal))(op.eResult)
        else
          null
      }
      else null
    }
    else null
  }

  def propagateBinOp[T,R](op: BinOp[T,R], x: Rep[T], y: Rep[T]): Rep[R] = {
    if (currentPass.config.constantPropagation) {
      if (x.isConst && y.isConst) {
        val xVal = valueFromRep(x)
        val yVal = valueFromRep(y)
        if (op.shouldPropagate(xVal, yVal))
          reifyObject(Const(op.applySeq(xVal, yVal))(op.eResult))
        else
          null
      }
      else null
    } else
      null
  }
}