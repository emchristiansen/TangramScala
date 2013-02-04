package tangram

///////////////////////////////////////////////////////////

/**
 * Represents a set of requirements we impose on
 * the generated wallpaper.  
 */
// TODO: (border: ImageBorder) should be (borderWidth: Int), as
// border color isn't an optimization constraint.
case class Constraints(
  /**
   * The larger dimension of each image must be
   * bigger than |minAbsoluteSize|.
   */
  minAbsoluteSize: Int,
  /**
   * Images should not end up drastically smaller than they were
   * originally. So we require:
   * finalWidth >= minRelativeSize * originalWidth
   * and similarly for height.
   */
  minRelativeSize: Double,
  /**
   * We don't want to severely crop images, so we require:
   * 1 / maxAspectWarp * finalAspect <= originalAspect <= maxAspectWarp * finalAspect
   */
  maxAspectWarp: Double,
  /**
   * The final size of the wallpaper.
   */
  wallpaperSize: RectangleSize,
  /**
   * The trim added around the images for aesthetic purposes.
   */
  border: ImageBorder) {
  require(minAbsoluteSize > 0)
  require(maxAspectWarp >= 1)
}

/**
 * Represents a series of relaxations on a particular constraint.
 * Some constraints cannot be satisfied, so it can be useful to gradually
 * relax them.
 */
trait Relaxations {
  /**
   * A stream of identical contraints.
   */
  def noRelaxations: Stream[Constraints]

  /**
   * A stream of increasingly relaxed constraints.
   */
  def relaxations: Stream[Constraints]
}

object Constraints {
  // It is useful to start with strong constraints and gradually
  // relax them if a solution is not initially found. 
  implicit class Constraints2Relaxations(self: Constraints) extends Relaxations {
    override def noRelaxations = 
      Stream.iterate(self)(identity)

    override def relaxations = {
      // TODO: This should eventually become adjustable.
      def relax(constraint: Constraints): Constraints = Constraints(
        (constraint.minAbsoluteSize * .95).round.toInt,
        constraint.minRelativeSize * .95,
        constraint.maxAspectWarp * 1.05,
        constraint.wallpaperSize,
        constraint.border)

      Stream.iterate(self)(relax)
    }
  }
}