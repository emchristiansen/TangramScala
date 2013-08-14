package tangram.style

///////////////////////////////////////////////////////////

/**
 * Anything that supports |plus| and has a zero element.
 */
trait Monoid[T] {
  def plus(that: T): T
  def zero: T
}

/**
 * Any Monoid that supports "*".
 */
trait SemiVectorSpace[T] extends Monoid[T] {
  def *(that: Double): T
}

/**
 * An abstract rectangle with non-zero area.
 */
case class RectangleSize(width: Int, height: Int) {
  require(width >= 0 && height >= 0)
}

object RectangleSize {
  implicit class RectangleSize2SemiVectorSpace(self: RectangleSize) extends SemiVectorSpace[RectangleSize] {
    override def plus(that: RectangleSize) = RectangleSize(
      self.width + that.width,
      self.height + that.height)
      
    override def zero = RectangleSize(0, 0)
    
    override def *(that: Double) = RectangleSize(
      (self.width * that).round.toInt,
      (self.height * that).round.toInt)
  }

  implicit class RectangleSizeOps(self: RectangleSize) {
    def aspect = self.width.toDouble / self.height.toDouble
  }
}