package photostream

import java.awt.Color
import java.awt.image.BufferedImage

trait Monoid[T] {
  def +(that: T): T
  def zero: T
}

trait SemiVectorSpace[T] extends Monoid[T] {
  def *(that: Double): T
}

case class RectangleSize(width: Int, height: Int) {
  require(width >= 0 && height >= 0)
}

object RectangleSize {
  implicit def implicitSemiVectorSpace(self: RectangleSize): SemiVectorSpace[RectangleSize] =
    new SemiVectorSpace[RectangleSize] {
      override def +(that: RectangleSize) = RectangleSize(
        self.width + that.width,
        self.height + that.height)
      override def zero = RectangleSize(0, 0)
      override def *(that: Double) = RectangleSize(
        (self.width * that).round.toInt,
        (self.height * that).round.toInt)
    }

  implicit def implicitRectangleLike(self: RectangleSize): RectangleLike = new RectangleLike {
    override def size = self
  }
}

trait RectangleLike {
  def size: RectangleSize
  def width = size.width
  def height = size.height
  def aspect = size.width.toDouble / size.height.toDouble
}

case class ImageBorder(val borderWidth: Int, val color: Color)

case class ResizedImage(
  originalImage: BufferedImage,
  width: Int,
  height: Int) {
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

object ResizedImage {
  def apply(originalImage: BufferedImage): ResizedImage = ResizedImage(
    originalImage,
    originalImage.getWidth,
    originalImage.getHeight)
}

case class BorderedResizedImage(val border: ImageBorder, val image: ResizedImage) {
  val width = 2 * border.borderWidth + image.width
  val height = 2 * border.borderWidth + image.height

  def size: RectangleSize = RectangleSize(width, height)

  def render: BufferedImage = {
    Preprocess.addBorder(border.borderWidth, border.color)(image.render)
  }

  def resize(newSize: RectangleSize): BorderedResizedImage = {
    val newWidth = newSize.width - 2 * border.borderWidth
    val newHeight = newSize.height - 2 * border.borderWidth
    copy(image = image.copy(width = newWidth, height = newHeight))
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

  implicit def implicitRectangleLike(self: BorderedResizedImage): RectangleLike = new RectangleLike {
    override def size = self.size
  }
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