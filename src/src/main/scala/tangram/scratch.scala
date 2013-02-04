package tangram;
//package photostream
//
//import com.twitter.util.Eval
//
//trait HiddenConversion[A, B] {
//  def convert: A => B
//}
//
//trait QuickImplicit[A] {
//  def original: A
//  def to[B](implicit conversion: A => B): B = conversion(original)
//  def hiddenTo[B](implicit conversion: HiddenConversion[A, B]): B =
//    conversion.convert(original)
//
//  // Like |to|, but the implicit conversion is found at
//  // runtime using Eval, rather than at compile time which is 
//  // usually the case.
//  def runtimeTo[B: Manifest]: B = {
//    val source = """
//import photostream._
//import photostream.Foo1._
//import photostream.Bar1._
//import photostream.Bar2._
//      
////implicitly[%s => %s]      
//implicitly[Int => Double]
//      """.format(original.getClass.getCanonicalName, implicitly[Manifest[B]])
//    println(source)
//    val conversion = (new Eval).apply[Any](source)
//    println("here")
//    println(conversion)
//    sys.error("asdf")
////    conversion(original)
//  }
//}
//
//object QuickImplicit {
//  implicit def aToQuickImplicit[A](a: A) = new QuickImplicit[A] {
//    override def original = a
//  }
//}
//
////trait RuntimeImplicit[A] {
////  def original: A
////  def runtimeTo[B]: A = {
////    sys.error("asdf")
////  }
////}
//
//trait FooLike
//
//case class Foo1()
//
//object Foo1 {
//  implicit def foo1ToFooLike(foo1: Foo1) = new FooLike {}
//}
//
//trait BarLike
//
//case class Bar1()
//
//object Bar1 {
//  implicit def foo1ToBar1(foo1: Foo1) = Bar1()
//
//  implicit def bar1ToBarLike(bar1: Bar1) = new BarLike {}
//}
//
//case class Bar2()
//
//object Bar2 {
//  implicit def foo1H2Bar2 = new HiddenConversion[Foo1, Bar2] {
//    override def convert = (foo1: Foo1) => Bar2()
//  }
//}
//
//object RuntimeImplicit {
//  //  val source = """
//  //      import nebula._;
//  //      import nebula.smallBaseline._;
//  //      import nebula.wideBaseline._;
//  //      import spark.SparkContext;
//  //      import SparkContext._;
//  //      
//  //      val value: %s = { %s };
//  //      value""".format(implicitly[Manifest[A]], expression)
//  //
//  //  (new Eval).apply[A](source)
//
//  trait TemetNosce {
//    val manifest: Manifest[_]
//  }
//
//  trait IntLike extends TemetNosce {
//    def int: Int
//  }
//
//  trait BooleanLike extends TemetNosce {
//    def boolean: Boolean
//  }
//
//  implicit def int2IntLike(i: Int) = new IntLike {
//    val manifest = implicitly[Manifest[Int]]
//    def int = i
//  }
//
//  implicit def boolean2BooleanLike(b: Boolean) = new BooleanLike {
//    val manifest = implicitly[Manifest[Boolean]]
//    def boolean = b
//  }
//
//}