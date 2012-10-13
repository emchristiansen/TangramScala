package photostream

///////////////////////////////////////////////////////////////////////////////

sealed trait Split

object HorizontalSplit extends Split

object VerticalSplit extends Split

///////////////////////////////////////////////////////////////////////////////

sealed trait PartitionTree

case class PartitionNode(
  val image: BorderedResizedImage,
  val split: Split,
  val left: Option[PartitionTree],
  val right: Option[PartitionTree]) extends PartitionTree {
  // Verify the tree is a legal size.
  split match {
    case HorizontalSplit => {
      left.map()
      
    }
  }
}

case class PartitionLeaf(
  val image: BorderedResizedImage) extends PartitionTree

object PartitionTree {
  def size(tree: PartitionTree): RectangleSize = tree match {
    case PartitionLeaf(image) => image.size
    case PartitionNode(
      image,
      _,
      left,
      right) => {
      val extraWidth = right.map(tree => size(tree).width).getOrElse(0)
      val extraHeight = left.map(tree => size(tree).height).getOrElse(0)
      RectangleSize(image.width + extraWidth, image.height + extraHeight)
    }
  }

  def render(tree: PartitionTree): Wallpaper = tree match {
    case PartitionLeaf(image) => {
      val wallpaper = Wallpaper(image.width, image.height)
      wallpaper.insert(image, Position(0, 0))
    }
    case PartitionNode(
      image,
      split,
      left,
      right) => {
      val rootWallpaper = Wallpaper(image)
      val leftWallpaper = for (l <- left) yield render(l)
      val rightWallpaper = for (r <- right) yield render(r)

      def horizontal(left: Wallpaper, rightOption: Option[Wallpaper]): Wallpaper = rightOption match {
        case Some(right) => Wallpaper.concatenateHorizontal(left, right)
        case None => left
      }

      def vertical(top: Wallpaper, bottomOption: Option[Wallpaper]): Wallpaper = bottomOption match {
        case Some(bottom) => Wallpaper.concatenateVertical(top, bottom)
        case None => top
      }

      split match {
        case HorizontalSplit =>
          vertical(
            horizontal(rootWallpaper, rightWallpaper),
            leftWallpaper)
        case VerticalSplit =>
          horizontal(
            vertical(rootWallpaper, leftWallpaper),
            rightWallpaper)
      }
    }
  }
}