package scalan.meta

import scalan.meta.ScalanAst.SEntityDef

class EntityTests extends BaseMetaTests with Examples {

  val cols = parseModule(colsVirtModule)
  context.addModule(cols)
  val eCollection = cols.getEntity("Collection")
  val eColOverArray = cols.getEntity("ColOverArray")
  val ePairCollection = cols.getEntity("PairCollection")
  val ePairOfCols = cols.getEntity("PairOfCols")

  describe("SEntityDef ops") {
    it("getInheritedTypes") {
      val types = eColOverArray.getInheritedTypes
      types.map(_.name) shouldBe(List("Collection", "Def"))
    }
    it("inherits") {
      eColOverArray.inherits("Def") shouldBe(true)
      eColOverArray.inherits("Def1") shouldBe(false)
      eColOverArray.inherits("Collection") shouldBe(true)
      eColOverArray.inherits("Iters") shouldBe(false)
    }
    it("collectMethodsInBody") {
      ePairOfCols.collectItemsInBody(_.isAbstract).map(_.item.name) shouldBe(List())
      ePairOfCols.collectItemsInBody(!_.isAbstract).map(_.item.name) shouldBe(List("ls", "rs", "eL", "eR", "length", "apply", "map"))
    }
    it("collectMethodsFromAncestors") {
      ePairOfCols.collectMethodsFromAncestors(_.isAbstract).map(_.item.name) shouldBe(List("eL", "eR", "ls", "rs", "eA", "arr", "length", "apply", "map", "eA", "arr", "length", "apply", "map"))
      ePairOfCols.collectMethodsFromAncestors(!_.isAbstract).map(_.item.name) shouldBe(List())
    }
    it("asSeenFrom") {

    }
  }

  describe("linearization") {
    // see http://www.scala-lang.org/files/archive/spec/2.13/05-classes-and-objects.html#class-linearization
    it("SEntityDef.linearization") {
      ePairCollection.linearization.map(_.name) shouldBe(List("PairCollection", "Collection"))
      ePairOfCols.linearization.map(_.name) shouldBe(List("PairOfCols", "PairCollection", "Collection"))
      eColOverArray.linearization.map(_.name) shouldBe(List("ColOverArray", "Collection"))
    }

    def testRefinementProp(entC: SEntityDef, entD: SEntityDef, anyLin: List[SEntityDef]): Unit = {
      assert(entC.inherits(entD.name))
      val posC = anyLin.map(_.name).indexOf(entC.name)
      val posD = anyLin.map(_.name).indexOf(entD.name)
      assert(posC < posD, "if C is a subclass of D, then C precedes D in any linearization where both C and D occur")
    }

    it("refinement property") {
      testRefinementProp(ePairCollection, eCollection, ePairCollection.linearization)
      testRefinementProp(ePairCollection, eCollection, ePairOfCols.linearization)
    }

    // TODO linearization suffix property test
    // assert(true, "A linearization of a class always contains the linearization of its direct superclass as a suffix.")
  }

  describe("SEntityMember") {
    def testMatches(e1: SEntityDef, e2: SEntityDef, name: String): Unit = {
      val l1 = e1.findMemberInBody(name).get
      val l2 = e2.findMemberInBody(name).get
      assert(l1.matches(l2), s"Method '$name' don't match in entities ${e1.name} and ${e2.name}")
    }

    it("matches") {
      testMatches(ePairOfCols, eCollection, "length")
      testMatches(ePairOfCols, eCollection, "apply")
      testMatches(eColOverArray, eCollection, "map")
      testMatches(eColOverArray, eCollection, "eA")
      testMatches(ePairOfCols, ePairCollection, "eL")
      //TODO  testMatches(ePairOfCols, eCollection, "map")
    }
  }
}
