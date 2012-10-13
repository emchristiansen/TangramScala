package photostream

import java.awt.Color
import java.awt.image.BufferedImage

case class RectangleSize(width: Int, height: Int) {
  require(width > 0 && height > 0)
}

case class ImageBorder(val borderWidth: Int, val color: Color)

case class ResizedImage(
  val originalImage: BufferedImage,
  val width: Int,
  val height: Int) {
  require(originalImage.getWidth > 0)
  require(originalImage.getHeight > 0)
  require(width > 0)
  require(height > 0)

  def render: BufferedImage = {
    val newImage = new BufferedImage(
      width,
      height,
      BufferedImage.TYPE_INT_ARGB)

    // TODO: I suspect this may not work.
    val graphics = newImage.createGraphics
    graphics.drawImage(originalImage, 0, 0, width, height, null)
    newImage
  }
}

case class BorderedResizedImage(val border: ImageBorder, val image: ResizedImage) {
  val width = 2 * border.borderWidth + image.width
  val height = 2 * border.borderWidth + image.height  
  
  def size: RectangleSize = RectangleSize(width, height)
  
  def render: BufferedImage = {
    Preprocess.addBorder(border.borderWidth, border.color)(image.render)
  }
}

object BorderedResizedImage {
  def resizeToFit(
    border: ImageBorder, 
    finalSize: RectangleSize, 
    image: BufferedImage): BorderedResizedImage = BorderedResizedImage(
      border,
      ResizedImage(
        image,
        finalSize.width - 2 * border.borderWidth,
        finalSize.height - 2 * border.borderWidth))
  
}

object Preprocess {
  def addBorder(
    borderWidth: Int,
    borderColor: Color)(
      image: BufferedImage): BufferedImage = {
    val newWidth = image.getWidth + 2 * borderWidth
    val newHeight = image.getHeight + 2 * borderWidth
    val newImage = new BufferedImage(
      newWidth,
      newHeight,
      BufferedImage.TYPE_INT_ARGB)
    val graphics = newImage.createGraphics
    graphics.setColor(borderColor)
    graphics.fillRect(0, 0, newWidth, newHeight)
    graphics.drawImage(image, borderWidth, borderWidth, null)
    newImage
  }
}