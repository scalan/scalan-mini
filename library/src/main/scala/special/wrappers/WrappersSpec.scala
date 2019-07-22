package special.wrappers {
  import scalan._

  trait WrappersSpec extends Base { self: Library =>
    import WOption._;
    import WRType._;
    import WSpecialPredef._;
    import WrapSpecBase._;
    trait WrapSpecBase extends Def[WrapSpecBase] with WrapSpec;
    trait OptionWrapSpec extends WrapSpecBase {
      def get[A](xs: Rep[WOption[A]]): Rep[A] = xs.get;
      @NeverInline def getOrElse[A](xs: Rep[WOption[A]], default: Rep[Thunk[A]]): Rep[A] = delayInvoke;
      def map[A, B](xs: Rep[WOption[A]], f: Rep[scala.Function1[A, B]]): Rep[WOption[B]] = xs.map[B](f);
      def flatMap[A, B](xs: Rep[WOption[A]], f: Rep[scala.Function1[A, WOption[B]]]): Rep[WOption[B]] = xs.flatMap[B](f);
      def filter[A](xs: Rep[WOption[A]], f: Rep[scala.Function1[A, Boolean]]): Rep[WOption[A]] = xs.filter(f);
      def isDefined[A](xs: Rep[WOption[A]]): Rep[Boolean] = xs.isDefined;
      def isEmpty[A](xs: Rep[WOption[A]]): Rep[Boolean] = xs.isEmpty;
      @NeverInline def fold[A, B](xs: Rep[WOption[A]], ifEmpty: Rep[Thunk[B]], f: Rep[scala.Function1[A, B]]): Rep[B] = delayInvoke
    };
    trait SpecialPredefWrapSpec extends WrapSpecBase {
      def loopUntil[A](s1: Rep[A], isMatch: Rep[scala.Function1[A, Boolean]], step: Rep[scala.Function1[A, A]]): Rep[A] = RWSpecialPredef.loopUntil[A](s1, isMatch, step);
      def cast[A](v: Rep[Any])(implicit cA: Elem[A]): Rep[WOption[A]] = RWSpecialPredef.cast[A](v);
      def some[A](x: Rep[A]): Rep[WOption[A]] = RWSpecialPredef.some[A](x);
      def none[A](implicit cA: Elem[A]): Rep[WOption[A]] = RWSpecialPredef.none[A];
      def optionGetOrElse[A](opt: Rep[WOption[A]], default: Rep[A]): Rep[A] = RWSpecialPredef.optionGetOrElse[A](opt, default)
    };
    trait RTypeWrapSpec extends WrapSpecBase {
      def name[T](d: Rep[WRType[T]]): Rep[String] = d.name
    };
    trait WrapSpecBaseCompanion;
    trait ArrayWrapSpecCompanion;
    trait OptionWrapSpecCompanion;
    trait SpecialPredefWrapSpecCompanion;
    trait RTypeWrapSpecCompanion
  }
}