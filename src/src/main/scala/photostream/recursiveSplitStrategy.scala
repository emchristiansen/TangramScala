package photostream

import java.awt.Color

import Run.UpdateWallpaper

object RecursiveSplitStrategy {
  type SplitRecursive = (RectangleSize, Stream[UnusedImage]) => (PartitionTree, Stream[UnusedImage])

  def breakIntoThirds: SplitRecursive = (partitionSize, images) => {
    require(images.nonEmpty)

    val border = ImageBorder(1, Color.white)

    val minDimension = 300

    val UnusedImage(headImage, _) #:: tailImages = images
    val unscaledImage = BorderedResizedImage(border, ResizedImage(headImage))

    val RectangleSize(partitionWidth, partitionHeight) = partitionSize
    if (partitionWidth <= minDimension || partitionHeight <= minDimension) {
      (PartitionLeaf(unscaledImage.resize(partitionSize)),
        tailImages)
    } else {
      val (split, scaleFactor) =
        if (partitionSize.aspect > unscaledImage.aspect) {
          // This image will go on the left.
          val scaleFactor = (2.0 / 3.0) * partitionHeight.toDouble / (unscaledImage.height.toDouble)
          (VerticalSplit, scaleFactor)
        } else {
          // This image will go on the top.
          val scaleFactor = (2.0 / 3.0) * partitionWidth.toDouble / (unscaledImage.width.toDouble)
          (HorizontalSplit, scaleFactor)
        }

      val newSize = unscaledImage.size * scaleFactor
      val scaledImage = unscaledImage.resize(newSize)

      val rightWidth = partitionWidth - scaledImage.width
      val leftHeight = partitionHeight - scaledImage.height

      val (leftSubtree, rightSubtree, unusedImages) = split match {
        case VerticalSplit => {
          val (leftSubtree, tailImages2) = breakIntoThirds(
            RectangleSize(scaledImage.width, leftHeight),
            tailImages)
          val (rightSubtree, tailImages3) = breakIntoThirds(
            RectangleSize(rightWidth, partitionHeight),
            tailImages2)
          (leftSubtree, rightSubtree, tailImages3)
        }
        case HorizontalSplit => {
          val (leftSubtree, tailImages2) = breakIntoThirds(
            RectangleSize(partitionWidth, leftHeight),
            tailImages)
          val (rightSubtree, tailImages3) = breakIntoThirds(
            RectangleSize(rightWidth, scaledImage.height),
            tailImages2)
          (leftSubtree, rightSubtree, tailImages3)
        }
      }

      (PartitionNode(
        PartitionLeaf(scaledImage),
        split,
        Some(leftSubtree),
        Some(rightSubtree)),
        unusedImages)
    }
  }

  def full: UpdateWallpaper = (wallpaper, images) => {
    val partitionSize = RectangleSize(wallpaper.width, wallpaper.height)

    val (partitionTree, remainingImages) = breakIntoThirds(partitionSize, images)

    println(partitionTree)
    println(partitionTree.size)

    (partitionTree.wallpaper, remainingImages)
  }
}