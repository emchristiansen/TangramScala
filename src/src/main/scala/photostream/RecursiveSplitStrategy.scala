package photostream

import Run.UpdateWallpaper
import java.awt.Color

sealed trait Split
object HorizontalSplit extends Split
object VerticalSplit extends Split

sealed trait PartitionTree
case class PartitionNode(
  val image: BorderedResizedImage,
  val split: Split,
  val left: Option[PartitionTree],
  val right: Option[PartitionTree]) extends PartitionTree
case class PartitionLeaf(
  val image: BorderedResizedImage) extends PartitionTree

case class RectangleSize(width: Int, height: Int) {
  require(width > 0 && height > 0)
}

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

object RecursiveSplitStrategy {
  type SplitRecursive = (RectangleSize, Stream[UnusedImage]) => (PartitionTree, Stream[UnusedImage])

  def breakIntoThirds: SplitRecursive = (partitionSize, images) => {
    require(images.nonEmpty)

    val border = ImageBorder(2, Color.black)
    
    val minDimension = 300

    val UnusedImage(headImage, _) #:: tailImages = images

    val RectangleSize(width, height) = partitionSize
    if (width <= minDimension || height <= minDimension) {
      (PartitionLeaf(BorderedResizedImage(
         border,
         ResizedImage(headImage, width, height))), 
       tailImages)
    } else {
      val imageAspect = headImage.getWidth.toDouble / headImage.getHeight.toDouble
      val partitionAspect = width.toDouble / height.toDouble
      if (partitionAspect > imageAspect) {
        // This image will go on the left.
        val scaleFactor = height.toDouble / headImage.getHeight.toDouble
        val rootWidth = (headImage.getWidth * scaleFactor).toInt
        val remainingWidth = width - rootWidth
        val (subtree, remainingImages) =
          breakIntoThirds(RectangleSize(remainingWidth, height), tailImages)
        val tree = PartitionNode(
          BorderedResizedImage(
            border,
            ResizedImage(headImage, rootWidth, height)),
          HorizontalSplit,
          None,
          Some(subtree))
        (tree, remainingImages)
      } else {
        // This image will go on the top.
        val scaleFactor = width.toDouble / headImage.getWidth.toDouble
        val rootHeight = (headImage.getHeight * scaleFactor).toInt
        val remainingHeight = height - rootHeight
        val (subtree, remainingImages) =
          breakIntoThirds(RectangleSize(width, remainingHeight), tailImages)
        val tree = PartitionNode(
          BorderedResizedImage(
            border,
            ResizedImage(headImage, width, rootHeight)),
          VerticalSplit,
          Some(subtree),
          None)
        (tree, remainingImages)
      }
    }
  }

  def full: UpdateWallpaper = (wallpaper, images) => {
    val partitionSize = RectangleSize(wallpaper.width, wallpaper.height)

    val (partitionTree, remainingImages) = breakIntoThirds(partitionSize, images)

    println(partitionTree)
    println(PartitionTree.size(partitionTree))

    (PartitionTree.render(partitionTree), remainingImages)
  }
}