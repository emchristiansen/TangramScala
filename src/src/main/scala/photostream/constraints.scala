package photostream

///////////////////////////////////////////////////////////

// |Constraints| represents a set of requirements we impose on
// the generated wallpaper.
// TODO: (border: ImageBorder) should be (borderWidth: Int), as
// border color isn't an optimization constraint.
case class Constraints(
  // The larger dimension of each image must be
  // bigger than |minAbsoluteSize|.
  minAbsoluteSize: Int,
  // Images should not end up much smaller than they were
  // originally. So we require:
  // finalWidth >= minRelativeSize * originalWidth
  // and similarly for height.
  minRelativeSize: Double,
  // We don't want to severely crop images, so we require:
  // 1 / maxAspectWarp * finalAspect <= originalAspect <= maxAspectWarp * finalAspect
  maxAspectWarp: Double,
  // The final size of the wallpaper.
  wallpaperSize: RectangleSize,
  border: ImageBorder) {
  require(minAbsoluteSize > 0)
  require(maxAspectWarp >= 1)
}

object Constraints {
  // It is useful to start with strong constraints and gradually
  // relax them if a solution is not initially found.
  implicit def addRelaxations(self: Constraints) = new {
    def noRelaxations: Stream[Constraints] = Stream.iterate(self)(identity)
    
    def relaxations: Stream[Constraints] = {
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