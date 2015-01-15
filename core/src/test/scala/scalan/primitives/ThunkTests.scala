package scalan.primitives

import java.io.File
import java.lang.reflect.Method
import scala.language.reflectiveCalls
import scalan.BaseTests
import scalan.common.{SegmentsDslExp, SegmentsDsl}

import scalan.{Scalan, ScalanCtxExp, ScalanCtxSeq}

class ThunkTests extends BaseTests {
  val prefix = new File("test-out/scalan/primitives/thunk/")

  trait TestContext extends ScalanCtxExp {
    val subfolder: String
    def emit(name: String, ss: Exp[_]*) =
      emitDepGraph(ss.toList, new File(prefix + subfolder, s"/$name.dot"), false)
  }

  trait MyProg extends Scalan {
    val subfolder = "/myprog"

    lazy val t1 = fun { (in: Rep[Int]) =>
      Thunk { in }
    }

    lazy val t2 = fun { (in: Rep[Int]) =>
      Thunk { in + 1 }
    }

    lazy val t3 = fun { (in: Rep[Int]) =>
      Thunk { in + in + 1 }
    }

    lazy val t4 = fun { (in: Rep[Int]) =>
      Thunk { in + Thunk { in + 1 }.force }
    }
    lazy val t5 = fun { (in: Rep[Int]) =>
      Thunk { in + 1 }.force + Thunk { in + 1 }.force
    }
    lazy val t6 = fun { (in: Rep[Int]) =>
      Thunk { Thunk {in + 1}.force + 1 }.force + 1
    }

    def f7(x: => Rep[Int]) = Thunk { x }
    lazy val t7 = fun { (in: Rep[Int]) =>
      f7 {in + 1}
    }
    def f8(x: Rep[Int]) = Thunk { x }
    lazy val t8 = fun { (in: Rep[Int]) =>
      f8 {in + 1}
    }

    lazy val t9 = fun { (in: Rep[Int]) =>
      Thunk { (in + 1) + Thunk { in + 1 }.force }
    }

    lazy val t10 = fun { (in: Rep[Int]) =>
      Thunk { Thunk { Thunk { in + 1 }}}.force.force
    }
  }

  test("thunksWithoutInlining") {
    val ctx = new TestContext with  MyProg {
      override def isInvokeEnabled(d: Def[_], m: Method) = true

      def test() = {
        assert(!isInlineThunksOnForce, "precondition for tests")

        {
          val Def(Lambda(_, _, x, Def(th@ThunkDef(res, sch)))) = t1
          assert(x == res && sch.isEmpty && th.freeVars.isEmpty)
        }
        {
          val Def(Lambda(_, _, x, Def(th@ThunkDef(res, sch)))) = t2
          assert(sch.size == 2 && th.freeVars.contains(x))
        }
        {
          val Def(Lambda(_, _, x, Def(th@ThunkDef(res, sch)))) = t3
          assert(sch.size == 3 && th.freeVars.contains(x))
        }
        {
          val Def(Lambda(_, _, x, Def(th@ThunkDef(res, sch)))) = t4
          assert(sch.size == 3 && th.freeVars.contains(x) && th.freeVars.size == 2)
        }
        {
          val Def(Lambda(_, _, x, Def(th@ThunkDef(res, sch)))) = t7
          assert(sch.size == 2 && th.freeVars.contains(x))
        }
        {
          val Def(Lambda(_, _, x, Def(th@ThunkDef(res, sch)))) = t8
          assert(sch.size == 1 && th.freeVars.contains(x) && th.freeVars.size == 2)
        }
      }
    }
    ctx.test
    ctx.emit("t1", ctx.t1)
    ctx.emit("t2", ctx.t2)
    ctx.emit("t3", ctx.t3)
    ctx.emit("t2t3", ctx.t2, ctx.t3) // see not shared constants
    ctx.emit("t4", ctx.t4)
    ctx.emit("t5", ctx.t5)
    ctx.emit("t6", ctx.t6)
    ctx.emit("t7", ctx.t7)
    ctx.emit("t8", ctx.t8)
    ctx.emit("t9", ctx.t9)
    ctx.emit("t10", ctx.t10)
  }

  test("thunksWithInlining") {
    val ctx = new TestContext with  MyProg {
      override def isInvokeEnabled(d: Def[_], m: Method) = true
      isInlineThunksOnForce = true

      def test() = {
        assert(isInlineThunksOnForce, "precondition for tests")
      }
    }
    ctx.test
    ctx.emit("t1_inl", ctx.t1)
    ctx.emit("t2_inl", ctx.t2)
    ctx.emit("t3_inl", ctx.t3)
    ctx.emit("t2t3_inl", ctx.t2, ctx.t3) // see not shared constants
    ctx.emit("t4_inl", ctx.t4)
    ctx.emit("t5_inl", ctx.t5)
    ctx.emit("t6_inl", ctx.t6)
    ctx.emit("t7_inl", ctx.t7)
    ctx.emit("t8_inl", ctx.t8)
    ctx.emit("t9_inl", ctx.t9)
    ctx.emit("t10_inl", ctx.t10)
  }

  trait MyDomainProg extends Scalan with SegmentsDsl {
    val subfolder = "/mydomainprog"

    lazy val t1 = fun { (in: Rep[Int]) =>
      Thunk { Interval(in, in) }.force.length
    }
  }

  test("thunksOfDomainTypes") {
    val ctx = new TestContext with SegmentsDslExp with MyDomainProg {
      override def isInvokeEnabled(d: Def[_], m: Method) = true
      isInlineThunksOnForce = false
//      override def rewriteDef[T](d: Def[T]) = d match {
//        case DefBlock(res, )
//      }

      def test() = {
        assert(!isInlineThunksOnForce, ": precondition for tests")

      }
    }
    ctx.test
    ctx.emit("t1", ctx.t1)
  }

}
