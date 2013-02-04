package tangram

import reflect.runtime.universe
import reflect.runtime.universe._
import scala.tools.reflect.ToolBox

///////////////////////////////////////////////////////////

object Eval {
  /**
   * Container for blocks of import statements in Scala source code.
   */
  case class Imports(packages: Set[String])

  object Imports {
    def apply(packages: String*): Imports = Imports(packages.toSet)
    
    implicit def toSetString(self: Imports): Set[String] = self.packages
  }

  /**
   * Default imports needed to build against Tangram.
   */
  implicit val allImports = Imports(
      "tangram._", 
      "tangram.streams._", 
      "tangram.styles._")

  /**
   * Pimp to add a set of imports to the top of a source code string.
   */
  implicit class AddImportsToSource(source: String) {
    def addImports(implicit imports: Imports): String = {
      val formattedImports = imports map { x => s"import ${x}" }
      val importString = formattedImports.toList.sorted.mkString("\n")
      importString + "\n\n" + source
    }
  }  
  
  ///////////////////////////////////////////////////////////
  
  /**
   * Represents the name of a type as a string.
   */
  case class TypeName[A: TypeTag](name: String) {
    assert(typeTag[A].tpe.toString == name)
    override def toString = name
  }

  object TypeName {
    implicit def typeName2String(typeName: TypeName[_]): String = typeName.name
  }

  /**
   * Convenience function to get the name of a type.
   */
  def typeName[A: TypeName] = implicitly[TypeName[A]]

  implicit def typeTag2TypeName[A: TypeTag]: TypeName[A] = 
    TypeName(typeTag[A].tpe.toString)

  ///////////////////////////////////////////////////////////

  /**
   * Checks the type of the given expression.
   * If the type check passes, returns a closure that can be run to evaluate
   * the expression.
   */
  def typeCheck[A: TypeName](expression: String): () => A = {
    val source = s"""
val result: ${typeName[A]} = {${expression}}
result
    """

    println(source)

    // From http://stackoverflow.com/questions/12122939/generating-a-class-from-string-and-instantiating-it-in-scala-2-10/12123609#12123609
    val cm = universe.runtimeMirror(getClass.getClassLoader)
    val toolbox = cm.mkToolBox()

    toolbox.compile(toolbox.parse(source)).asInstanceOf[() => A]
  }

  /**
   * Type checks and evaluates a given expression.
   */
  def eval[A: TypeName](expression: String): A = {
    typeCheck[A](expression).apply()
  }
}