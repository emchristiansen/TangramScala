package photostream

import java.awt.Color

import Run.UpdateWallpaper

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