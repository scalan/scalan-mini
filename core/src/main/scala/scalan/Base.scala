package scalan

import java.lang.reflect.{Method, Constructor => Constr}
import java.util.{HashMap, Objects, Arrays, function}

import configs.syntax._
import com.typesafe.config.{ConfigFactory, Config}
import scalan.OverloadHack.Overloaded1

import scala.annotation.implicitNotFound
import scala.annotation.unchecked.uncheckedVariance
import scala.collection.{mutable, TraversableOnce}
import scala.collection.mutable.ListBuffer
import scala.reflect.{ClassTag, classTag}
import scalan.compilation.GraphVizConfig
import scalan.util.{ReflectionUtil, NeighbourFunc, StringUtil}
import debox.{Buffer => DBuffer, Set => DSet}
import scala.reflect.runtime.universe._
import spire.syntax.all.cfor

/**
  * The Base trait houses common AST nodes. It also manages a list of encountered Definitions which
  * allows for common sub-expression elimination (CSE).
  */
trait Base { scalan: Scalan =>
  type |[+A, +B] = Either[A, B]
  type RFunc[-A,+B] = Rep[Function1[A,B]]
  type RPair[+A, +B] = Rep[(A,B)]

  // Consider if extra data should be Seq[Any] instead (change name in this case)
  class StagingException(message: String, cause: Throwable, val syms: Seq[Rep[_]]) extends
    RuntimeException(stagingExceptionMessage(message, syms), cause) {
    def this(message: String, syms: Seq[Rep[_]]) = this(message, null, syms)
  }

  class NotImplementedStagingException(message: String, syms: Seq[Rep[_]]) extends StagingException(message, null, syms)

  def ??? : Nothing = ???("Missing or incomplete implementation")
  def ???(value: Any, syms: Rep[_]*): Nothing = throw new NotImplementedStagingException(value.toString, syms)

  /** Helper methods to throw errors */
  def !!! : Nothing = !!!("should not be called")
  def !!!(msg: String, syms: Rep[_]*): Nothing = throw new StagingException(msg, syms)
  def !!!(msg: String, e: Throwable, syms: Rep[_]*): Nothing = throw new StagingException(msg, e, syms)

  /** Log warning message to the log.
    * This is default and simple implementation, which can be overriden.*/
  def logWarn(msg: => String): Unit = {
    println(msg)
  }

  /** Helper to type cast symbols. */
  @inline final def asRep[T](x: Rep[_]): Rep[T] = x.asInstanceOf[Rep[T]]

  @inline implicit def liftToRep[A:Elem](x: A): Rep[A] = toRep(x)

  /** Base type for all graph nodes (aka symbol definitions). Each graph node or definition
    * represent one operation node of the data flow graph.
    */
  trait Def[+T] extends Product {
    private[scalan] var _nodeId: Int = freshId

    /** Unique id of the graph node assigned for each new instance using
      * `freshId` generator.
      * Doesn't participate in equality of this Def, thus definitions with
      * different ids may still be structurally equal.
      * Used to provide global Def numbering. */
    @inline def nodeId: Int = _nodeId

    /** Type of a resuting value produced by the operation represented by this definition.
      * For example, if this definition represents application of `+: (Int, Int) => Int` operation
      * then the result type is Int and `resultType` should return IntElement. */
    def resultType: Elem[T @uncheckedVariance]

    private var _self: Rep[T @uncheckedVariance] = _

    /** Reference to this definition created lazy on demand. */
    def self: Rep[T] = {
      if (_self == null) _self = freshSym(this)
      _self
    }

    private var _deps: Array[Sym] = _

    /** Dependencies of this definition from other definitions.
      * If definition is interpreted as an operation, then dependencies are arguments
      * of the operation.
      * If definition if compound (like Lambda of ThunkDef) then `deps` is equals to
      * free variables used in the body of the compound definition.
      * This array also refers to predecessors of this graph node, so it is used
      * to build topological ordering (execution schedule) of operations.
      * @return array of referencies to other definitions.*/
    final def deps: Array[Sym] = {
      if (_deps == null) {
        _deps = getDeps
      }
      _deps
    }

    /** Override to redefine how dependencies are computed.
      * For example, in `core` implementation this is overriden in Lambda and ThunkDef using freeVars. */
    protected def getDeps: Array[Sym] = syms

    private var _syms: Array[Sym] = _
    private var _elements: Array[Any] = _

    /** Generic method to extract `elements` and `syms` from this node.*/
    final private def initContent(): Unit = {
      val len = productArity
      _elements = new Array[Any](len + 1)
      _elements(0) = getClass
      val symsBuf = DBuffer.ofSize[Sym](len)
      cfor(0)(_ < len, _ + 1) { i =>
        val element = productElement(i)
        _elements(i + 1) = element
        Def.extractSyms(element, symsBuf)
      }
      _syms = symsBuf.toArray()
    }

    /** References to other nodes in this Def instance.
      * Note: This is different form `deps` for compound definitions like Lambda and ThunkDef. */
    def syms: Array[Sym] = {
      if (null == _syms) initContent()
      _syms
    }

    /** All data elements of this graph node to be used in structural equality.
      * @see equals where elements are used.*/
    def elements: Array[AnyRef] = {
      if (null == _elements) initContent()
      _elements.asInstanceOf[Array[AnyRef]]
    }

    /** Create a copy of this definition applying the given transformer to all `syms`. */
    def transform(t: Transformer): Def[T] =
      !!!(s"Cannot transfrom definition using transform($this)", self)

    /** Clone this definition transforming all symbols using `t`.
      * If new Def[A] is created, it is added to the graph with collapsing and rewriting.
      * Can be overriden to implement node-specific mirroring (see MethodCall).
      * @param  t  mapping of symbols to symbols (Rep[_] => Rep[_])
      * @return  symbol of the logical clone. If `d` don't contain symbols, then d.self is returned. */
    def mirror(t: Transformer): Rep[T] = {
      val newD = transform(t)
      reifyObject(newD)
    }

    /** Default equality of definitions.
      * Two definitions are equal if they have same `elements`. */
    override def equals(other: Any) = (this eq other.asInstanceOf[AnyRef]) || {
      val eq = canEqual(other) && Arrays.deepEquals(elements, other.asInstanceOf[Def[_]].elements)
      eq
    }

    private var _hashCode: Int = 0
    /** Computed once and saved to avoid repeated computations, which is not necessary
      * because definitions are immutable by default.
      * If some definition require mutability, this method can be overriden accordingly. */
    override def hashCode = {
      if (_hashCode == 0) {
        _hashCode = Arrays.deepHashCode(elements)
      }
      _hashCode
    }

    /** User readable string representation of this definition. (for debugging only) */
    override def toString = {
      val sb = new StringBuilder
      sb.append(productPrefix)
      sb.append("(")
      val iterator = productIterator
      if (iterator.hasNext) {
        StringUtil.deepAppend(sb, iterator.next)
      }
      while (iterator.hasNext) {
        sb.append(", ")
        StringUtil.deepAppend(sb, iterator.next)
      }
      sb.append(")")
      sb.toString
    }
  }

  object Def {
    def unapply[T](e: Rep[T]): Nullable[Def[T]] = def_unapply(e)

    /** Traverse `element` structure and extracts symbols to buf`. */
    final def extractSyms(element: Any, buf: DBuffer[Sym]): Unit = element match {
      case s: Sym =>
        buf += s
      case p: Product =>
        val len = p.productArity
        cfor(0)(_ < len, _ + 1) { i =>
          extractSyms(p.productElement(i), buf)
        }
      case xs: Seq[_] =>
        val len = xs.length
        cfor(0)(_ < len, _ + 1) { i =>
          extractSyms(xs(i), buf)
        }
      case _ =>
    }
  }

  /** Logical AND between two pattern matches of the save value `x`.
    * Can be used to construct patterns like `case P1 && P2 => ...` */
  object && {
    def unapply[T](x: T): Option[(T,T)] = Some((x, x))
  }

  /** Base class for virtualized instances of type companions.
    * Each virtualized entity type (trait or class) may have virtualized companion class. */
  abstract class CompanionDef[T] extends Def[T] {
    override def productArity = 0
    override def productElement(n: Int) = !!!(s"productElement($n) called, but productArity = 0", self)
    override def canEqual(other: Any) = other.isInstanceOf[CompanionDef[_]]
    override def mirror(t: Transformer): Rep[T] = self
  }

  /** Data type `ST` is liftable is there is Liftable[ST, T] instance for some type `T`.
    * Liftable typeclass allows to define which types can have values embedded as literals
    * into graph IR. */
  object Liftables {

    /** Base class for graph nodes which represent data values of liftable types
      * as literal nodes in the graph IR.
      * @tparam ST source type of the liftable value
      * @tparam T  virtualized type (aka IR type) corresponding to source type
      */
    trait LiftedConst[ST, T] extends Def[T] {
      /** Value of the source type embedded in this graph node. */
      def constValue: ST

      /** Evidence that constValue can be lifted to T */
      def liftable: Liftable[ST, T]

      /** This default implementation assumes there is no symbols in this node.
        * Can be overriden if it is not true for some ST. */
      override def mirror(t: Transformer): Rep[T] = self
    }

    /** Describes lifting data values of type ST (Source Type) to IR nodes of the correspoding staged type T.
      * In general T is different type obtained by virtualization procedure from ST.
      * However ST can be the same as T as is the case for Byte, Int, String etc.
      */
    @implicitNotFound(msg = "Cannot find implicit for Liftable[${ST},${T}].")
    trait Liftable[ST, T] {
      /** Type descriptor of the source type */
      def sourceType: RType[ST]
      /** Type descriptor of the IR type */
      def eW: Elem[T]

      /** Method to embedd source type instance into graph IR. */
      def lift(x: ST): Rep[T]

      /** Extract lifted value from graph node.
        * @return constValue from LiftedConst if `w` refers to such a node.
        * @throws StagingException if `w` is not refering to the appropritate LiftedConst node. */
      def unlift(w: Rep[T]): ST

      protected def unliftError(w: Rep[T]) =
        !!!(s"Cannot unlift simbol $w using $this")

      /** We assume only single Liftable[ST, T] implementation for every IR type `T`.
        * And all instances of it are equal. */
      override def hashCode(): Int = eW.hashCode() + 1 // to make Elem and Liftable differ
      override def equals(obj: Any): Boolean = super.equals(obj) || (obj match {
        case other: Liftable[_,_] => other.eW == eW
        case _ => false
      })
      override def toString: String = s"Liftable($eW)"
    }

    /** Casts untyped Liftable to typed one. */
    @inline final def asLiftable[ST,T](l: Liftable[_,_]): Liftable[ST,T] = l.asInstanceOf[Liftable[ST,T]]

    /** Shortcut alternative to `implicitly[Liftable[ST,T]]` */
    @inline final def liftable[ST, T](implicit lT: Liftable[ST,T]) = lT

    /** Given data value of source type `ST` and `Liftable` instance between `ST` and `T`,
      * produces `LiftedConst` node (some concrete implemenation) and returns it's symbol.
      * This is generic way to put any liftable data object into graph and then use
      * its symbol in other nodes. */
    @inline final def liftConst[ST,T](x: ST)(implicit lT: Liftable[ST,T]): Rep[T] = lT.lift(x)

    /** Liftable evidence for primitive (base) types (used in BaseElemLiftable). */
    class BaseLiftable[T](implicit val eW: Elem[T], override val sourceType: RType[T]) extends Liftable[T, T] {
      def lift(x: T) = toRep(x)
      def unlift(w: Rep[T]) = valueFromRep(w)
    }

    /** Liftable evidence between `(SA, SB)` and `(A, B)` types. */
    class PairLiftable[SA,SB,A,B](implicit lA: Liftable[SA, A], lB: Liftable[SB, B]) extends Liftable[(SA,SB), (A,B)] {
      val eW: Elem[(A, B)] = pairElement(lA.eW, lB.eW)
      override val sourceType: RType[(SA, SB)] = RType.pairRType(lA.sourceType, lB.sourceType)

      def lift(x: (SA, SB)): Rep[(A, B)] = Pair(lA.lift(x._1), lB.lift(x._2))
      def unlift(w: Rep[(A, B)]): (SA, SB) = { val Pair(wa, wb) = w; (lA.unlift(wa), lB.unlift(wb)) }
    }

    /** Every function can be lifted to the graph IR. */
    case class FuncConst[SA,SB,A,B](constValue: SA => SB)(implicit lA: Liftable[SA, A], lB: Liftable[SB, B])
          extends BaseDef[A => B]()(funcElement(lA.eW, lB.eW))
             with LiftedConst[SA => SB, A => B] {
      val liftable = Liftables.liftable[SA => SB, A => B]
    }

    class FuncLiftable[SA,SB,A,B](implicit lA: Liftable[SA, A], lB: Liftable[SB, B]) extends Liftable[SA => SB, A => B] {
      val eW: Elem[A => B] = funcElement(lA.eW, lB.eW)
      override val sourceType = RType.funcRType(lA.sourceType, lB.sourceType)
      def lift(srcF: SA => SB): Rep[A => B] = FuncConst[SA,SB,A,B](srcF)
      def unlift(f: Rep[A => B]): SA => SB = f match {
        case Def(FuncConst(srcF)) => srcF.asInstanceOf[SA => SB]
        case _ => unliftError(f)
      }
    }

    implicit lazy val BooleanIsLiftable = asLiftable[Boolean,Boolean](BooleanElement.liftable)
    implicit lazy val ByteIsLiftable    = asLiftable[Byte,Byte](ByteElement.liftable)
    implicit lazy val ShortIsLiftable   = asLiftable[Short,Short](ShortElement.liftable)
    implicit lazy val IntIsLiftable     = asLiftable[Int,Int](IntElement.liftable)
    implicit lazy val LongIsLiftable    = asLiftable[Long,Long](LongElement.liftable)
    implicit lazy val StringIsLiftable  = asLiftable[String,String](StringElement.liftable)
    implicit lazy val FloatIsLiftable   = asLiftable[Float,Float](FloatElement.liftable)
    implicit lazy val DoubleIsLiftable  = asLiftable[Double,Double](DoubleElement.liftable)
    implicit lazy val UnitIsLiftable    = asLiftable[Unit,Unit](UnitElement.liftable)
    implicit lazy val CharIsLiftable    = asLiftable[Char,Char](CharElement.liftable)

    implicit def PairIsLiftable[SA,SB,A,B]
        (implicit lA: Liftable[SA, A], lB: Liftable[SB, B]): Liftable[(SA, SB), (A, B)] =
      new PairLiftable[SA,SB,A,B]

    implicit def FuncIsLiftable[SA,SB,A,B]
        (implicit lA: Liftable[SA, A], lB: Liftable[SB, B]): Liftable[SA => SB, A => B] =
      new FuncLiftable[SA,SB,A,B]
  }

  /** Base class for all objects generated for virtualized types to support
    * staged evaluation machinery.
    * Each object contains definitions which can be imported when necessary.
    * All that objects are registered in `entityObjects` hash map,
    * which is done while IR cake is constructed.
    */
  class EntityObject(val entityName: String)

  private[this] val entityObjects = AVHashMap[String, EntityObject](300)

  @inline def getEntityObject(name: String): Nullable[EntityObject] = {
    entityObjects.get(name)
  }

  protected def registerEntityObject(name: String, obj: EntityObject): Unit = {
     assert(!entityObjects.containsKey(name), s"EntityObject for entity $name already registered")
     entityObjects.put(name, obj)
  }

  /** Whether IR type descriptors should be cached. */
  val cacheElems = true

  /** Whether Tup instances should be cached. */
  val cachePairs = true

  /** Abstract REPresentation of a computable value.
    * Default implementation is a simple lightweight reference to the corresponding definition.
    * Every Rep have direct access to its Def via `rhs` property.
    * Every Rep is typed, and the type is avaliable via `elem` property.
    * @see SingleRep
    */
  abstract class Rep[+T] {
    /** Type of the computed value represented by the node refered by this rep.*/
    def elem: Elem[T @uncheckedVariance]

    /** Unique name that can be used as variable name.*/
    def varName: String

    /** Node of the graph refered by this rep. */
    def rhs: Def[T]

    /** Most of the references are initialized when created.
      * These methods are used in core to assign new value for the reference.*/
    private[scalan] def assignDef[B >: T](d: Def[B]): Unit
    private[scalan] def assignDefFrom[B >: T](sym: Rep[B]): Unit

    /** Whether the underlying node is Placeholder. */
    @inline final def isPlaceholder: Boolean = rhs.isInstanceOf[Placeholder[_]]
    /** Whether the underlying node is Variable. */
    @inline final def isVar: Boolean = rhs.isInstanceOf[Variable[_]]
    /** Whether the underlying node is Const. */
    @inline final def isConst: Boolean = rhs.isInstanceOf[Const[_]]
    /** Whether the underlying node is Lambda. */
    @inline final def isLambda: Boolean = rhs.isInstanceOf[Lambda[_,_]]
    /** Is this reference of Companion type */
    @inline final def isCompanionType: Boolean = elem.isInstanceOf[CompanionElem[_]]

    /** Returns the string like `x45: Int = Const(10)` */
    def toStringWithDefinition: String
    def varNameWithType = varName + ":" + elem.name

    /** Build graph of nodes starting from this node, generate dot file,
      * and open it using system default viewer for dot extension. */
    def show(): Unit = show(defaultGraphVizConfig)
    def show(emitMetadata: Boolean): Unit = show(defaultGraphVizConfig.copy(emitMetadata = emitMetadata))
    def show(config: GraphVizConfig): Unit = showGraphs(this)(config)
  }

  /** Shortcut sinonim used as untyped reference to graph nodes (definitions). */
  type Sym = Rep[_]

  /** Base class for most predefined operations. */
  abstract class BaseDef[+T](implicit val resultType: Elem[T @uncheckedVariance]) extends Def[T]

  /** Default node type for embedding of literal values to graph IR.
    * This can be used or those types `T` when `Elem[T]` is defined,
    * but `Liftable[_,T]` is not, i.e. for non-liftable types.
    * @param  x   literal value
    * @param  eT  type descriptor of IR type T */
  case class Const[T](x: T)(implicit val eT: Elem[T]) extends BaseDef[T] {
    override def mirror(t: Transformer): Rep[T] = self
  }

  /** Node class for typed variables. In particular for lambda-bound variables.
    * @param varId   is independent from nodeId, shouldn't be used as node id.
    * @param eT      type descriptor of the variable type */
  case class Variable[T](varId: Int)(implicit eT: LElem[T]) extends Def[T] {
    override def resultType: Elem[T] = eT.value
    override def mirror(t: Transformer): Rep[T] = self
  }

  @inline def variable[T](implicit eT: LElem[T]): Rep[T] = Variable[T](freshId)

  /** Symbols may temporary refer to this node until their target node is updated. */
  case class Placeholder[T](eT: LElem[T]) extends Def[T] {
    def resultType: Elem[T] = eT.value
  }

  @inline def placeholder[T](implicit eT: LElem[T]): Rep[T] = freshSym[T](Placeholder[T](eT))

  /** Base class for Rep to Rep transformations.
    * Each transformer keeps a mapping data between original nodes and transformed nodes.
    */
  abstract class Transformer {
    /** Apply transformation. */
    def apply[A](x: Rep[A]): Rep[A]
    /** Whether this transformer is defined for the given node. */
    def isDefinedAt(x: Rep[_]): Boolean
    /** Set of nodes where this transformer is defined. */
    def domain: Set[Rep[_]]
    /** Transform a sequence of nodes into new sequence of nodes. */
    def apply[A](xs: Seq[Rep[A]]): Seq[Rep[A]] = {
      val len = xs.length
      val res = new Array[Rep[A]](len)
      cfor(0)(_ < len, _ + 1) { i =>
        res(i) = apply(xs(i))
      }
      res
    }
    /** Apply this transformer to the nodes present in the sequence,
      * and leave non-Rep items unchanged. */
    def apply(xs: Seq[Any])(implicit o: Overloaded1): Seq[Any] = {
      xs map (e => e match { case s: Rep[_] => apply(s); case _ => e })
    }
  }

  trait TransformerOps[Ctx <: Transformer] {
    def empty: Ctx
    def add[A](ctx: Ctx, kv: (Rep[A], Rep[A])): Ctx
    def merge(ctx1: Ctx, ctx2: Ctx): Ctx = ctx2.domain.foldLeft(ctx1) {
      case (t, s: Rep[a]) => add(t, (s, ctx2(s)))
    }
  }

  implicit class TransformerExtensions[Ctx <: Transformer](self: Ctx)(implicit ops: TransformerOps[Ctx]) {
    def +[A](kv: (Rep[A], Rep[A])) = ops.add(self, kv)
    def ++(kvs: Map[Rep[A], Rep[A]] forSome {type A}) = kvs.foldLeft(self)((ctx, kv) => ops.add(ctx, kv))
    def merge(other: Ctx): Ctx = ops.merge(self, other)
  }

  /** Prettyprint exception message */
  protected def stagingExceptionMessage(message: String, syms: Seq[Rep[_]]) = {
    // Skip syms already in the message, assume that's the only source for s<N>
    val symsNotInMessage = syms.map(_.toString).filterNot(message.contains)

    if (symsNotInMessage.isEmpty) {
      message
    } else {
      val between = if (message.isEmpty)
        ""
      else
        message.last match {
          // determine whether the message lacks ending punctuation
          case '.' | ';' | '!' | '?' => " "
          case _ => ". "
        }

      message + between + s"Sym${if (symsNotInMessage.length > 1) "s" else ""}: ${symsNotInMessage.mkString(", ")}"
    }
  }

  /** Variants of `owner` parameter of constructors of nested classes:
    * 1) predefined node classes are owned by IR cake (ScalanOwner)
    * 2) entity classes are owned by enclosing EntityObject */
  sealed trait OwnerKind
  case object NoOwner extends OwnerKind
  case object ScalanOwner extends OwnerKind
  case class  EntityObjectOwner(obj: EntityObject) extends OwnerKind

  /** Returns OwnerKind for the given constructor, using its first parameter. */
  protected def getOwnerKind(constructor: Constr[_]): OwnerKind = {
    val paramTypes = constructor.getParameterTypes
    val ownerParam =
      if (paramTypes.length == 0)
        NoOwner
      else {
        val firstParamClazz = paramTypes(0)
        if (firstParamClazz.getSuperclass == classOf[EntityObject]) {
          val className = firstParamClazz.getSimpleName
          val entityName = className.substring(0, className.length - 1)
          getEntityObject(entityName) match {
            case Nullable(obj) =>
              EntityObjectOwner(obj)
            case _ =>
              !!!(s"Unknown owner type $firstParamClazz")
          }
        } else {
          ScalanOwner
        }
      }
    ownerParam
  }

  import Liftables.LiftedConst

  /** Transforms this object into new one by applying `t` to every Rep inside
    * its structure. The structure is build out of Seq, Array, Option and Def values.
    * Other structure items remain unchanged and copied to the new instance.
    * @hotspot don't beautify the code */
  protected def transformProductParam(x: Any, t: Transformer): Any = x match {
    case (_: UnOp[_, _]) | (_: BinOp[_, _]) =>
      // allows use of context bounds in classes extending UnOp/BinOp.
      // Note that this must be overridden if some transformation _is_ needed (i.e. if the class contains Rep[_] somewhere)
      x
    case e: Rep[_] => t(e)
    case seq: Seq[_] =>
      val len = seq.length
      val res = new Array[AnyRef](len)
      cfor(0)(_ < len, _ + 1) { i => res(i) = transformProductParam(seq(i), t).asInstanceOf[AnyRef] }
      res: Seq[_]
    case arr: Array[_] =>
      val len = arr.length
      val res = new Array[AnyRef](len)
      cfor(0)(_ < len, _ + 1) { i => res(i) = transformProductParam(arr(i), t).asInstanceOf[AnyRef] }
      res
    case opt: Option[_] =>
      if (opt.isEmpty) None else Some(transformProductParam(opt.get, t))
    case d: Def[_] => d.mirror(t).rhs
    case x => x
  }

  /** Prepend owner parameter depending on its kind. */
  private[scalan] def addOwnerParameter(ownerType: OwnerKind, params: Seq[Any]): Seq[AnyRef] = {
    val finalParams = (ownerType match {
      case EntityObjectOwner(obj) => obj +: params
      case ScalanOwner => scalan +: params
      case NoOwner => params
    })
    finalParams.asInstanceOf[Seq[AnyRef]]
  }

  /** Implicit injection of new definition (graph node) into universum of
    * nodes with collapsing semantics. If there exists node `n` in this IR
    * such that `obj equals n`, then the value of `n.self` is returned, i.e.
    * the new node `obj` is collapsed with already existing one.
    * This has an effect of Common Subexpression Elimination (CSE) when
    * an expression tree is transformed to the graph.
    * After a reference to the node is obtained, global rewriting rules are
    * examined and the reference may be replaced with a new one.
    */
  implicit def reifyObject[A](obj: Def[A]): Rep[A] = {
    toExp(obj, obj.self)
  }

  /** Lifting of data values to IR nodes. */
  def toRep[A](x: A)(implicit eA: Elem[A]):Rep[A] = eA match {
    case _: BaseElem[_] => Const(x)
    case _: FuncElem[_, _] => Const(x)
    case pe: PairElem[a, b] =>
      val x1 = x.asInstanceOf[(a, b)]
      implicit val eA = pe.eFst
      implicit val eB = pe.eSnd
      Pair(toRep(x1._1), toRep(x1._2))
    case _ =>
      x match {
        // this may be called instead of reifyObject implicit in some cases
        case d: Base#Def[A @unchecked] => reifyObject(d.asInstanceOf[Def[A]])
        case _ => !!!(s"Don't know how to create Rep for $x with element $eA")
      }
  }

  /** Extract data value from Const node or throw an exception. */
  @inline final def valueFromRep[A](x: Rep[A]): A = x.rhs match {
    case Const(x) => x
    case _ => delayInvoke
  }

  def def_unapply[T](e: Rep[T]): Nullable[Def[T]] = new Nullable(e.rhs)

  object ExpWithElem {
    def unapply[T](s: Rep[T]): Nullable[(Rep[T],Elem[T])] = Nullable((s, s.elem))
  }

  /** A Rep is a symbolic reference used internally to refer to graph nodes.
    * Light weight stateless immutable reference to a graph node (Def[T]).
    * Two symbols are equal if they refer to the nodes with the same id,
    * which is due to Def unification means equal symbols refer to the same instance of Def.
    * */
  class SingleRep[+T] private[Base](private var _rhs: Def[T @uncheckedVariance]) extends Rep[T] {
    override def elem: Elem[T @uncheckedVariance] = _rhs.resultType
    override def rhs: Def[T] = _rhs

    private[scalan] def assignDefInternal[B >: T](d: Def[B]): Unit = {
      assert(_rhs.isInstanceOf[Placeholder[_]])
      assert(_rhs.nodeId > 0)
      val tab = _symbolTable
      val oldId = _rhs.nodeId
      if (tab(oldId) eq this) {
        tab.update(oldId, null)
      }
      _rhs = d.asInstanceOf[Def[T]]
    }

    private[scalan] def assignDef[B >: T](d: Def[B]): Unit = {
      assignDefInternal(d)
      updateSymbolTable(this, d)
    }

    private[scalan] def assignDefFrom[B >: T](sym: Rep[B]): Unit = {
      assignDefInternal(sym.rhs)
    }

    private var _adapter: T @uncheckedVariance = _
    def adapter: T @uncheckedVariance = _adapter
    def adapter_=(a: T @uncheckedVariance) = { _adapter = a }

    /** Helper method that lazily creates and attaches Adapter to this node reference.
      * The adapter is created conditionally and on demand.
      */
    final def getAdapter[S >: T](isInstanceOfT: Boolean, createAdapter: Rep[S] => T @uncheckedVariance): T = {
      if (isInstanceOfT) _rhs.asInstanceOf[T]
      else {
        val adapter = _adapter
        if (adapter == null) {
          _adapter = createAdapter(this)
        }
        _adapter
      }
    }

    override def varName = "s" + _rhs._nodeId
    override def toString = varName
    override def toStringWithDefinition = varNameWithType + s" = ${_rhs}"

    override def equals(obj: scala.Any): Boolean = (this eq obj.asInstanceOf[AnyRef]) || (obj match {
      case other: SingleRep[_] => _rhs._nodeId == other.rhs._nodeId
      case _ => false
    })

    override def hashCode(): Int = _rhs._nodeId
  }

  private var currId: Int = 0
  @inline final def freshId: Int = { currId += 1; currId }

  @inline final def freshSym[T](d: Def[T]): Rep[T] = {
    updateSymbolTable(null, d)
  }

  /** Should be invoked to reset IR global node counter. */
  @inline final private[scalan] def resetIdCounter() = { currId = 0 }

  /** Create or find symbol in the table which refers to the given node.
    * The d.nodeId is the index in the _symbolTable which is DBuffer (backed by Array)
    * @return   new of existing symbol
    * @hotspot  the method should be allocation-free (make it sure by examining the generated Java code)
    */
  final def updateSymbolTable[T](s: Rep[T], d: Def[T]): Rep[T] = {
    val id = d.nodeId
    val tab = _symbolTable  // perf optimization
    val delta = id - tab.length
    if (delta < 0) {
      val sym = tab.apply(id)
      if (sym == null) {
        val newSym = if (s == null) new SingleRep(d) else s  // we really want this allocation to happen only when necessary
        tab.update(id, newSym)
        newSym
      } else {
        // don't create new symbol, but check invariant condition on existing one
        assert(sym.rhs.nodeId == id, s"Each symbol should refer to correct node, but was $sym -> ${sym.rhs}")
        sym.asInstanceOf[Rep[T]]
      }
    } else {
      // grow table
      cfor(0)(_ < delta, _ + 1) { _ => tab.append(null) }
      val sym = if (s == null) new SingleRep(d) else s
      tab += sym
      assert(tab.length == id + 1)
      sym
    }
  }

  @inline final def getSym(id: Int): Sym = _symbolTable(id)

  val nInitialDefs = 10000
  private[this] val _symbolTable: DBuffer[Sym] = DBuffer.ofSize(nInitialDefs)
  private[this] var _globalDefs = AVHashMap[Def[_], Def[_]](nInitialDefs)

  protected var globalThunkSym: Rep[_] = placeholder[Int] // we could use any type here

  def defCount = _globalDefs.hashMap.size

  private val _intZero = MutableLazy(0: Rep[Int])
  @inline final def IntZero = _intZero.value

  def resetContext() = {
    _globalDefs = AVHashMap[Def[_], Def[_]](nInitialDefs)
    _symbolTable.clear()
    _symbolTable.splice(0, DBuffer.ofSize[Sym](nInitialDefs))
    resetIdCounter()
    globalThunkSym = placeholder[Int]
    tuplesCache.clear()
    _intZero.reset()
    onReset()
  }

  /** Called during resetContext() operation after the core context state has been reset.
   * Derived classes can override to define application specific initialization.
   * Don't forget to call super method in the beginning of your overriding method. */
  protected def onReset(): Unit = {
  }

  def findGlobalDefinition[T](d: Def[T]): Rep[T] = {
    val existingOpt = _globalDefs.get(d)
    if (existingOpt.isDefined)
      existingOpt.get.self.asInstanceOf[Rep[T]]
    else
      null
  }

  /** Lookup `d` in the heap of nodes. If the lookup is successfull, then `d.self`
    * reference is returned. If the node is not found in the heap, then it is added
    * and `d.self` reference is returned.
    * @param  d       node to be added to the head of nodes
    * @param  newSym  producer of the reference to be used as the reference to `d` node.
    * @return         return a reference to `d` node in the heap
    * @hotspot */
  def findOrCreateDefinition[T](d: Def[T], newSym: => Rep[T]): Rep[T] = {
    val optScope = thunkStack.top
    var sym = optScope match {
      case Nullable(scope) =>
        scope.findDef(d)
      case _ =>
        findGlobalDefinition(d)
    }
    if (sym == null) {
      sym = createDefinition(optScope, newSym, d)
    }
//    assert(te.rhs == d, s"${if (te !) "Found" else "Created"} unequal definition ${te.rhs} with symbol ${te.sym.toStringWithType} for $d")
    sym
  }

  def createDefinition[T](s: Rep[T], d: Def[T]): Rep[T] =
    createDefinition(thunkStack.top, s, d)

  protected def createDefinition[T](optScope: Nullable[ThunkScope], s: Rep[T], d: Def[T]): Rep[T] = {
    assert(_symbolTable(d.nodeId).rhs.nodeId == d.nodeId)
    assert(s.rhs eq d, s"Inconsistent Sym -> Def pair $s -> $d")
    optScope match {
      case Nullable(scope) =>
        scope += s
      case _ =>
        _globalDefs.put(d, d)
    }
    s
  }

  /**
    * Updates the universe of symbols and definitions, then rewrites until fix-point
    * @param d A new graph node to add to the universe
    * @param newSym A symbol that will be used if d doesn't exist in the universe
    * @tparam T
    * @return The symbol of the graph which is semantically(up to rewrites) equivalent to d
    */
  protected[scalan] def toExp[T](d: Def[T], newSym: => Rep[T]): Rep[T] = {
    var res = findOrCreateDefinition(d, newSym)
    var currSym = res
    var currDef = d
    do {
      currSym = res
      val ns = rewriteDef(currSym.rhs).asInstanceOf[Rep[T]]
      if (null == ns) return currSym
      res = ns
      currDef = ns.rhs
    } while (res != currSym)
    res
  }

  object IdSupply {
    private var _nextId = 0

    def nextId = {
      _nextId += 1
      _nextId
    }
  }
}

