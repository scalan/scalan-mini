package special.collections

import scala.collection.mutable.ArrayBuffer
import org.scalacheck.util.Buildable

import scala.collection.mutable
import org.scalacheck.{Arbitrary, Gen}
import scalan._
import special.collection.{Coll, CollBuilder, CollOverArray, CollOverArrayBuilder, PairColl}

import scala.reflect.ClassTag
import scala.util.Random

trait CollGens { testSuite =>
  import Gen._
  val builder: CollBuilder = new CollOverArrayBuilder
  val monoid = builder.Monoids.intPlusMonoid
  val valGen = choose(-100, 100)
  val byteGen = choose[Byte](-100, 100)
  val indexGen = choose(0, 100)
  val replacedGen = choose(0, 100)
  val lenGen = choose(0, 100)

  def getArrayGen[T](valGen: Gen[T], count: Int = 100)
                    (implicit evb: Buildable[T,Array[T]], evt: Array[T] => Traversable[T]): Gen[Array[T]] = {
    containerOfN[Array, T](count, valGen)
  }

  def getCollOverArrayGen[T: RType](valGen: Gen[T], count: Int = 100): Gen[Coll[T]] = {
    containerOfN[Array, T](count, valGen).map(builder.fromArray(_))
  }

  def getCollReplGen[T: RType](lenGen: Gen[Int], valGen: Gen[T], count: Int = 100): Gen[Coll[T]] = {
    for { l <- lenGen; v <- valGen } yield builder.replicate(l, v)
  }

  def getCollViewGen[A: RType](valGen: Gen[Coll[A]]): Gen[Coll[A]] = {
    valGen.map(builder.makeView(_, identity[A]))
  }

  def getCollViewGen[A, B: RType](valGen: Gen[Coll[A]], f: A => B): Gen[Coll[B]] = {
    valGen.map(builder.makeView(_, f))
  }

  def getCollPairGenFinal[A: RType, B: RType](collGenLeft: Gen[Coll[A]], collGenRight: Gen[Coll[B]]): Gen[PairColl[A, B]] = {
    for { left <- collGenLeft; right <- collGenRight } yield builder.pairColl(left, right)
  }

  def getCollPairGenRight[A: RType, B: RType, C: RType](collGenLeft: Gen[Coll[A]], collGenRight: Gen[PairColl[B, C]]): Gen[PairColl[A, (B, C)]] = {
    for { left <- collGenLeft; right <- collGenRight } yield builder.pairColl(left, right)
  }

  def getCollPairGenLeft[A: RType, B: RType, C: RType](collGenLeft: Gen[PairColl[A, B]], collGenRight: Gen[Coll[C]]): Gen[PairColl[(A, B), C]] = {
    for { left <- collGenLeft; right <- collGenRight } yield builder.pairColl(left, right)
  }

  def getCollPairGenBoth[A: RType, B: RType, C: RType, D: RType](collGenLeft: Gen[PairColl[A, B]], collGenRight: Gen[PairColl[C, D]]): Gen[PairColl[(A, B), (C, D)]] = {
    for { left <- collGenLeft; right <- collGenRight } yield builder.pairColl(left, right)
  }

  def getSuperGen[T: RType](length: Int = 1, collGen: Gen[Coll[T]]): Gen[PairColl[_, _]] = {
    length match {
      case 0 => {
        return Gen.oneOf(getCollPairGenFinal(collGen, collGen),
          getCollPairGenFinal(collGen, collGen))
      }
      case _ => {
        getSuperGen(length - 1, collGen) match {
          case (lg: Gen[PairColl[RType[_], RType[_]]]) => {
            getSuperGen(length - 1, collGen) match {
              case (rg: Gen[PairColl[RType[_], RType[_]]]) => {
                val gen = Gen.oneOf(
                  getCollPairGenFinal(collGen, collGen),
                  getCollPairGenLeft(lg, collGen),
                  getCollPairGenRight(collGen, rg),
                  getCollPairGenBoth(lg, rg),
                )
                return gen
              }
              case _ => throw new RuntimeException("Invalid rGen")
            }
          }
          case _ => throw new RuntimeException("Invalid lGen")
        }
      }
    }
  }

  val bytesArrayGen: Gen[Array[Byte]] = getArrayGen[Byte](byteGen) //containerOfN[Array, Byte](100, byteGen)
  val arrayGen: Gen[Array[Int]] = getArrayGen[Int](valGen) //containerOfN[Array, Int](100, valGen)
  val indexesGen = containerOfN[Array, Int](10, indexGen).map(arr => builder.fromArray(arr.distinct.sorted))

  val collOverArrayGen = getCollOverArrayGen(valGen) //arrayGen.map(builder.fromArray(_))
  // val collsOverArrayGen = for { _ <- lenGen } yield containerOfN[Array, Int](100, valGen).map(builder.fromArray(_))
  val bytesOverArrayGen = getCollOverArrayGen(byteGen)
  val replCollGen = getCollReplGen(lenGen, valGen)
  val replBytesCollGen = getCollReplGen(lenGen, byteGen)


  val lazyCollGen = getCollViewGen(collOverArrayGen)
  val lazyByteGen = getCollViewGen(bytesOverArrayGen)

  def easyFunction(arg: Int): Int = arg * 20 + 300
  def inverseEasyFunction(arg: Int): Int = (arg - 300) / 20

  val lazyFuncCollGen = getCollViewGen[Int, Int](collOverArrayGen, easyFunction)
  val lazyUnFuncCollGen = getCollViewGen[Int, Int](lazyFuncCollGen, inverseEasyFunction)

  //val collsOfCollsArr = builder.fromItems(collsOverArrayGen) //(for { coll <- collOverArrayGen } yield coll).map(x => builder.fromItems(x))
  val collsOfCollsRepl = (for { len <- lenGen; coll <- replCollGen } yield builder.replicate(len, coll)).map(x => builder.fromItems(x))
  val collsOfCollsLazy = (for { coll <- Gen.oneOf(lazyCollGen, lazyUnFuncCollGen) } yield builder.makeView(coll, identity[Int])).map(x => builder.fromItems(x))
  val pairColls = for { coll1 <- Gen.oneOf(collsOfCollsRepl, collsOfCollsLazy); coll2 <- Gen.oneOf(collsOfCollsRepl, collsOfCollsLazy)}
    yield builder.pairColl(coll1, coll2)

  val collsOfCollsGen = Gen.oneOf(collsOfCollsRepl, collsOfCollsLazy, pairColls)
  //val collsOfCollsMixedGen = (for {_ <- lenGen} yield Gen.oneOf(collsOfCollsGen, collsOfCollsRepl, collsOfCollsLazy)

  val collGen = Gen.oneOf(collOverArrayGen, replCollGen, lazyCollGen, lazyUnFuncCollGen)
  val bytesGen = Gen.oneOf(bytesOverArrayGen, replBytesCollGen, lazyByteGen)

  val innerGen = Gen.oneOf(collOverArrayGen, replCollGen)
  // val collOfCollsGen = collGen.map(builder.fromItems(_))

  val superGen = getSuperGen(5, Gen.oneOf(collOverArrayGen, replCollGen))

  def generateFinalArray[T: RType](valGen: Gen[T]): CollOverArray[T] = {
    val item = containerOfN[Array, T](100, valGen).sample.getOrElse(Array())
    new CollOverArray[T](item)
  }

  implicit val arbColl = Arbitrary(collGen)
  implicit val arbBytes = Arbitrary(bytesGen)

  def eq0(x: Int) = x == 0
  def lt0(x: Int) = x < 0
  def plus(acc: Int, x: Int): Int = acc + x
  val plusF = (p: (Int,Int)) => plus(p._1, p._2)
  val predF = (p: (Int,Int)) => plus(p._1, p._2) > 0
  def inc(x: Int) = x + 1

  def complexFunction(arg: Int): Int = {
    var i = 0
    var res = 0
    while (i < 10) {
      res += arg - i
      i += 1
    }
    res
  }

  implicit def buildableColl[T:RType] = new Buildable[T,Coll[T]] {
    def builder = new mutable.Builder[T,Coll[T]] {
      val al = new ArrayBuffer[T]
      def +=(x: T) = {
        al += x
        this
      }
      def clear() = al.clear()
      def result() = testSuite.builder.fromArray(al.toArray)
    }
  }

  implicit def traversableColl[T](coll: Coll[T]): mutable.Traversable[T] = coll.toArray

}
