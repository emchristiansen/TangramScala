package photostream.styles

import photostream._
import RectangleSize._


///////////////////////////////////////////////////////////

sealed trait Split

object HorizontalSplit extends Split

object VerticalSplit extends Split

///////////////////////////////////////////////////////////

sealed trait SplitTree

case class SplitLeaf(image: BorderedResizedImage) extends SplitTree

case class SplitNode(
  first: SplitTree,
  second: SplitTree,
  split: Split) extends SplitTree

///////////////////////////////////////////////////////////

object SplitTree {
  import PartitionLeaf._
  import PartitionNode._  
  
  implicit def implicitHasWallpaper(self: SplitTree): HasWallpaper = self match {
    case self: SplitLeaf => implicitly[SplitLeaf => HasWallpaper].apply(self)
    case self: SplitNode => implicitly[SplitNode => HasWallpaper].apply(self)
  }
}

object SplitLeaf {
  implicit def implicitHasWallpaper(self: SplitLeaf): HasWallpaper = new HasWallpaper {
    override def wallpaper = Wallpaper(self.image)
  }
}

object SplitNode {
  implicit def implicitHasWallpaper(self: SplitNode): HasWallpaper = new HasWallpaper {
    override def wallpaper = {
      val first = self.first.wallpaper
      val second = self.second.wallpaper
      
      self.split match {
        case HorizontalSplit => Wallpaper.concatenateHorizontal(first, second)
        case VerticalSplit => Wallpaper.concatenateVertical(first, second)
      }
    }
  }
}