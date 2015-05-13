package scalan.effects

import scalan.monads.MonadsDsl

trait StateExamples extends MonadsDsl { self =>
  implicit val F = stateMonad[Int]
  import F.toMonadic

  def zipWithIndex[A:Elem](as: Rep[Array[A]]): Rep[Array[(Int,A)]] =
    as.foldLeft(F.unit(SArray.empty[(Int, A)]))({ p =>
      val Pair(acc, a) = p
      for {
        xs <- acc
        n  <- State.get[Int]
        _  <- State.set(n + 1)
      } yield (n, a) :: xs
    }).run(0)._1.reverse

  lazy val zipWithIndexW = fun { xs: Arr[Double] => zipWithIndex(xs) }
}
