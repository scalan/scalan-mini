package special.collection

import scala.reflect.ClassTag
import scalan.meta.RType

trait Costed[Val] {
  def builder: CostedBuilder
  def value: Val
  def cost: Int
  def dataSize: Long
}

trait CostedPrim[Val] extends Costed[Val] {
  def value: Val
  def cost: Int
  def dataSize: Long
}

trait CostedPair[L,R] extends Costed[(L,R)] {
  def l: Costed[L]
  def r: Costed[R]
}

trait CostedSum[L,R] extends Costed[Either[L, R]] {
  def value: Either[L, R]
  def left: Costed[Unit]
  def right: Costed[Unit]
}

trait CostedFunc[Env,Arg,Res] extends Costed[Arg => Res]  {
  def envCosted: Costed[Env]
  def func: Costed[Arg] => Costed[Res]
  def cost: Int
  def dataSize: Long
}

trait CostedCol[Item] extends Costed[Col[Item]] {
  def values: Col[Item]
  def costs: Col[Int]
  def sizes: Col[Long]
  def valuesCost: Int
  def mapCosted[Res](f: Costed[Item] => Costed[Res]): CostedCol[Res]
  def filterCosted(f: Costed[Item] => Costed[Boolean]): CostedCol[Item]
  def foldCosted[B](zero: Costed[B], op: Costed[(B, Item)] => Costed[B]): Costed[B]
}

trait CostedPairCol[L,R] extends Costed[Col[(L,R)]] {
  def ls: Costed[Col[L]]
  def rs: Costed[Col[R]]
}

trait CostedNestedCol[Item] extends Costed[Col[Col[Item]]] {
  def rows: Col[Costed[Col[Item]]]
}

/** NOTE: Option is a special case of Either, such that Option[T] is isomorphic to Either[Unit, T].
  * Keeping this in mind, we however define constructions for Option separately. */
trait CostedOption[T] extends Costed[Option[T]] {
  def get: Costed[T]
  def getOrElse(default: Costed[T]): Costed[T]
  def fold[B](ifEmpty: Costed[B], f: Costed[T => B]): Costed[B]
  def isEmpty: Costed[Boolean]
  def isDefined: Costed[Boolean]
  def filter(p: Costed[T => Boolean]): Costed[Option[T]]
  def flatMap[B](f: Costed[T => Option[B]]): Costed[Option[B]]
  def map[B](f: Costed[T => B]): Costed[Option[B]]
}

trait CostedBuilder {
  def ConstructTupleCost: Int = 1
  def ConstructSumCost: Int = 1
  def SelectFieldCost: Int = 1
  def SumTagSize: Long = 1
  def costedValue[T](x: T, optCost: Option[Int])(implicit cT: RType[T]): Costed[T]
  def defaultValue[T](valueType: RType[T]): T
  def monoidBuilder: MonoidBuilder
  def mkCostedPrim[T](value: T, cost: Int, size: Long): CostedPrim[T]
  def mkCostedPair[L,R](first: Costed[L], second: Costed[R]): CostedPair[L,R]
  def mkCostedSum[L,R](value: Either[L, R], left: Costed[Unit], right: Costed[Unit]): CostedSum[L, R]
  def mkCostedFunc[Env,Arg,Res](envCosted: Costed[Env], func: Costed[Arg] => Costed[Res], cost: Int, dataSize: Long): CostedFunc[Env, Arg, Res]
  def mkCostedCol[T](values: Col[T], costs: Col[Int], sizes: Col[Long], valuesCost: Int): CostedCol[T]
  def mkCostedPairCol[L,R](ls: Costed[Col[L]], rs: Costed[Col[R]]): CostedPairCol[L,R]
  def mkCostedNestedCol[Item](rows: Col[Costed[Col[Item]]])(implicit cItem: ClassTag[Item]): CostedNestedCol[Item]
  def mkCostedSome[T](costedValue: Costed[T]): CostedOption[T]
  def mkCostedNone[T](cost: Int)(implicit eT: RType[T]): CostedOption[T]
  def mkCostedOption[T](value: Option[T], none: Costed[Unit], some: Costed[Unit]): CostedOption[T]
}


