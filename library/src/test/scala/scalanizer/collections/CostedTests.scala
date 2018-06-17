package scalanizer.collections

import scala.collection.mutable
import scala.language.reflectiveCalls

class CostedTests extends BaseCostedTests {

  def measure[T](nIters: Int)(action: Int => Unit): Unit = {
    for (i <- 1 to nIters) {
      val start = System.currentTimeMillis()
      val res = action(i)
      val end = System.currentTimeMillis()
      val iterTime = end - start
      println(s"Iter $i: $iterTime ms")
    }
  }
  lazy val ctx = new Ctx { }
  import ctx._

  def buildGraph[T](nIters: Int, name: String)(action: Int => Rep[T]) = {
    val buf = mutable.ArrayBuilder.make[Rep[T]]()
    measure(nIters) { i =>
      buf += action(i)
    }
    ctx.emit(name, buf.result(): _*)
  }


  test("measure: plus const propagation") {
    buildGraph(10, "measure_plus_const") { i =>
      plus(i * 1000, 1000)
    }
  }

  test("plus fresh var") {
    buildGraph(10, "measure_plus_var") { i =>
      plus(fresh[Int], 1000)
    }
  }

  test("measure: dataCost") {
    buildGraph(10, "measure_dataCost") { i =>
      val data = Range(0, 20).foldLeft[Rep[Any]](toRep(i))((y, k) => Pair(y, k))
      result(dataCost(data))
    }
  }

  test("data cost") {
    ctx.emit("dataCost",
      result(dataCost(Pair(10, 20.toByte))),
      result(dataCost(Pair(30, Pair(40.toByte, 50L))))
    )
  }

  test("split") {
    ctx.emit("split",
      split(fun { x: Rep[(Int, Byte)] => dataCost(x) }),
      split(fun { x: Rep[Int] => dataCost(Pair(x, 20.toByte)) })
    )
  }

  test("split arrays") {
    ctx.emit("split_arrays",
      split(fun { in: Rep[(WArray[Int], Byte)] =>
        dataCost(in)
      })
    )
  }

  test("measure: split arrays") {
    buildGraph(10, "measure_split_arrays") { i =>
      var res: Rep[Any] = null
      for (k <- 1 to 1000) {
        res = split(fun { in: Rep[(WArray[Int], Byte)] =>
          val Pair(x, b) = in
          dataCost(Pair(x, b + i.toByte))
        })
      }
      res
    }
  }

  test("split pair arrays") {
    ctx.emit("split_pair_arrays",
      split(fun { in: Rep[(WArray[(Int, Short)], Byte)] =>
        dataCost(in)
      })
    )
    ctx.emit("split_pair_arrays2",
      split(fun { in: Rep[(WArray[(Int, (Short, Boolean))], Byte)] =>
        dataCost(in)
      })
    )
  }

  test("split nested arrays") {
    ctx.emit("split_nested_arrays",
      split(fun { in: Rep[(WArray[WArray[Int]], Byte)] =>
        dataCost(in)
      })
    )
  }

  test("split nested pair arrays") {
    ctx.emit("split_nested_pair_arrays",
      split(fun { in: Rep[(WArray[WArray[(Int, Short)]], Byte)] =>
        dataCost(in)
      })
    )
  }

  test("split nested nested arrays") {
    ctx.emit("split_nested_nested_arrays",
      split(fun { in: Rep[(WArray[WArray[WArray[Int]]], Byte)] =>
        dataCost(in)
      })
    )
  }

}
