package scalan.collection {
  import scalan._

  trait ConcreteCosts extends Base { self: Library =>
    abstract class CostedPrim[Val](val value: Rep[Val], val cost: Rep[Long]) extends Costed[Val] {
      def builder: Rep[ConcreteCostedBuilder] = ConcreteCostedBuilder()
    };
    abstract class CostedPair[L, R](val l: Rep[L], val r: Rep[R], val cost: Rep[Long]) extends Costed[scala.Tuple2[L, R]] {
      def builder: Rep[ConcreteCostedBuilder] = ConcreteCostedBuilder();
      def value: Rep[scala.Tuple2[L, R]] = Pair(CostedPair.this.l, CostedPair.this.r)
    };
    abstract class CostedArray[Item](val arr: Rep[Col[Costed[Item]]]) extends Costed[WArray[Item]] {
      implicit def eItem: Elem[Item] = eVal.eItem
      def builder: Rep[ConcreteCostedBuilder] = ConcreteCostedBuilder();
      def value: Rep[WArray[Item]] = CostedArray.this.arr.map[Item](fun(((c: Rep[Costed[Item]]) => c.value))).arr;
      def cost: Rep[Long] = CostedArray.this.arr.map[Long](fun(((c: Rep[Costed[Item]]) => c.cost))).fold[Long](toRep(0L.asInstanceOf[Long]))(fun(((in: Rep[scala.Tuple2[Long, Long]]) => {
        val x: Rep[Long] = in._1;
        val y: Rep[Long] = in._2;
        x.+(y)
      })))
    };
    abstract class ConcreteCostedBuilder extends CostedBuilder;
    trait CostedPrimCompanion;
    trait CostedPairCompanion;
    trait CostedArrayCompanion;
    trait ConcreteCostedBuilderCompanion
  }
}