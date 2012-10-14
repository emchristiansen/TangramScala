package photostream

import RectangleSize._

///////////////////////////////////////////////////////////

sealed trait Split

object HorizontalSplit extends Split

object VerticalSplit extends Split

///////////////////////////////////////////////////////////

sealed trait PartitionTree

case class PartitionLeaf(
  image: BorderedResizedImage) extends PartitionTree

// TODO: Rename left -> vertical and right -> horizontal
case class PartitionNode(
  leaf: PartitionLeaf,
  split: Split,
  left: Option[PartitionTree],
  right: Option[PartitionTree]) extends PartitionTree {
  // Verify the tree is a legal size.
  (left, right, split) match {
    case (None, None, _) => Unit
    case (None, Some(right), _) => require(leaf.height == right.height)
    case (Some(left), None, _) => require(leaf.width == left.width)
    case (Some(left), Some(right), VerticalSplit) =>
      require(leaf.height + left.height == right.height)
    case (Some(left), Some(right), HorizontalSplit) =>
      require(leaf.width + right.width == left.width)
  }
}

///////////////////////////////////////////////////////////

object PartitionTree {
  import PartitionLeaf._
  import PartitionNode._
  
  implicit def implicitRectangleLike(self: PartitionTree): RectangleLike = self match {
    case self: PartitionLeaf => implicitly[PartitionLeaf => RectangleLike].apply(self)
    case self: PartitionNode => implicitly[PartitionNode => RectangleLike].apply(self)
  }

  implicit def implicitHasWallpaper(self: PartitionTree): HasWallpaper = self match {
    case self: PartitionLeaf => implicitly[PartitionLeaf => HasWallpaper].apply(self)
    case self: PartitionNode => implicitly[PartitionNode => HasWallpaper].apply(self)
  }
}

object PartitionLeaf {
  implicit def implicitRectangleLike(self: PartitionLeaf): RectangleLike = new RectangleLike {
    override def size = self.image.size
  }

  implicit def implicitHasWallpaper(self: PartitionLeaf): HasWallpaper = new HasWallpaper {
    override def wallpaper = Wallpaper(self.image)
  }
}

object PartitionNode {
  implicit def implicitRectangleLike(self: PartitionNode): RectangleLike = new RectangleLike {
    override def size = {
      val extraWidthAndHeight = RectangleSize(
        self.right.map(_.size.width).getOrElse(0),
        self.left.map(_.size.height).getOrElse(0))

      self.leaf.size + extraWidthAndHeight
    }
  }

  implicit def implicitHasWallpaper(self: PartitionNode): HasWallpaper = new HasWallpaper {
    override def wallpaper = {
      val leafWallpaper = self.leaf.wallpaper
      val leftWallpaper = for (left <- self.left) yield left.wallpaper
      val rightWallpaper = for (right <- self.right) yield right.wallpaper

      def horizontal(left: Wallpaper, rightOption: Option[Wallpaper]): Wallpaper = rightOption match {
        case Some(right) => Wallpaper.concatenateHorizontal(left, right)
        case None => left
      }

      def vertical(top: Wallpaper, bottomOption: Option[Wallpaper]): Wallpaper = bottomOption match {
        case Some(bottom) => Wallpaper.concatenateVertical(top, bottom)
        case None => top
      }

      val wallpaper = self.split match {
        case HorizontalSplit => vertical(
          horizontal(leafWallpaper, rightWallpaper),
          leftWallpaper)
        case VerticalSplit => horizontal(
          vertical(leafWallpaper, leftWallpaper),
          rightWallpaper)
      }
      println(wallpaper.size)
      wallpaper
    }
  }
}

