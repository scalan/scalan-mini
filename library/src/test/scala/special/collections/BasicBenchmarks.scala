package special.collections

import org.scalameter.api._
import spire.syntax.all.cfor
import spire.util.Opt

trait BasicBenchmarkCases extends BenchmarkGens { suite: Bench[Double] =>
  performance of "Seq" in {
    var res: Seq[Int] = null
    measure method "Nil" in {
      using(sizes) in { case n =>
        cfor(0)(_ < n, _ + 1) { _ => res = Nil }
      }
    }
    measure method "empty" in {
      using(sizes) in { case n =>
        cfor(0)(_ < n, _ + 1) { _ => res = Seq.empty }
      }
    }
    measure method "apply" in {
      using(sizes) in { case n =>
        cfor(0)(_ < n, _ + 1) { _ => res = Seq() }
      }
    }
  }

  performance of "Map" in {
    var res: Map[Int,Int] = null
    measure method "empty" in {
      using(sizes) in { case n =>
        cfor(0)(_ < n, _ + 1) { _ => res = Map.empty[Int,Int] }
      }
    }
    measure method "apply" in {
      using(sizes) in { case n =>
        cfor(0)(_ < n, _ + 1) { _ =>
          res = Map[Int,Int]()
        }
      }
    }
  }

  performance of "for" in {
    var cell: Int = 0
    measure method "foreach" in {
      using(arrays) in { case (xs, _) =>
        xs.foreach { x => cell = x }
      }
    }
    measure method "cfor" in {
      using(arrays) in { case (xs, _) =>
        cfor(0)(_ < xs.length, _ + 1) { i => cell = xs(i) }
      }
    }
  }

  performance of "Seq vs Array" in {
    var res: Seq[Int] = null
    var head: Int = 0
    var tail: Seq[Int] = null
    measure method "Seq(...)" in {
      using(sizes) in { n =>
        cfor(0)(_ < n, _ + 1) { _ =>
          res = Seq(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
          head = res.head
          tail = res.tail
        }
      }
    }
    measure method "Array(...)" in {
      using(sizes) in { n =>
        cfor(0)(_ < n, _ + 1) { _ =>
          res = Array(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
          head = res.head
          tail = res.tail
        }
      }
    }
  }

  object PositiveInt {
    def unapply(n: Int): Option[Int] =
      if (n > 0) Some(n) else None
  }

  object PositiveIntOpt {
    def unapply(n: Int): Opt[Int] =
      if (n > 0) Opt(n) else Opt.empty
  }

  performance of "Option vs Opt" in {
    var res: Int = 0
    var resOpt: Int = 0
    measure method "PositiveInt.unapply" in {
      using(sizes) in { n =>
        cfor(0)(_ < n, _ + 1) { i =>
          res = i match {
            case PositiveInt(p) if p == 100 => p
            case _ => i
          }
        }
      }
    }
    measure method "PositiveIntOpt.unapply" in {
      using(sizes) in { n =>
        cfor(0)(_ < n, _ + 1) { i =>
          resOpt = i match {
            case PositiveIntOpt(p) if p == 100 => p
            case _ => i
          }
        }
      }
    }
  }
}

object FastBasicBenchmark extends Bench.LocalTime with BasicBenchmarkCases {
}

