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

case class ImageBorder(val width: Int, val color: Color) {
  require(width >= 0)
}

case class ResizedImage(
  originalImage: BufferedImage,
  width: Int,
  height: Int) {
  require(originalImage.getWidth > 0)
  require(originalImage.getHeight > 0)
  require(width > 0)
  require(height > 0)

  // We crop, we don't change the aspect ratio.
  def render: BufferedImage = {
    def scale(image: BufferedImage, scaleFactor: Double): BufferedImage = {
      val width = (image.getWidth * scaleFactor).round.toInt
      val height = (image.getHeight * scaleFactor).round.toInt
      
      val newImage = new BufferedImage(
        width,
        height,
        BufferedImage.TYPE_INT_ARGB)

      val graphics = newImage.createGraphics
      graphics.drawImage(originalImage, 0, 0, width, height, null)
      newImage
    }

    val originalSize = RectangleSize(originalImage.getWidth, originalImage.getHeight)
    val newSize = RectangleSize(width, height)

    val scaleFactor = if (originalSize.aspect > newSize.aspect) {
      // Make the heights match.
      newSize.height.toDouble / originalSize.height
    } else {
      // Make the widths match.
      newSize.width.toDouble / originalSize.width
    }

    val scaled = scale(originalImage, scaleFactor)

    // Crop off the extra bits.
    val extraWidth = scaled.getWidth - newSize.width
    val extraHeight = scaled.getHeight - newSize.height
    scaled.getSubimage(extraWidth / 2, extraHeight / 2, newSize.width, newSize.height)
  }

  //  def render: BufferedImage = {
  //    val newImage = new BufferedImage(
  //      width,
  //      height,
  //      BufferedImage.TYPE_INT_ARGB)
  //
  //    // TODO: I suspect this may not work.
  //    val graphics = newImage.createGraphics
  //    graphics.drawImage(originalImage, 0, 0, width, height, null)
  //    newImage
  //  }
}

object ResizedImage {
  def apply(originalImage: BufferedImage): ResizedImage = ResizedImage(
    originalImage,
    originalImage.getWidth,
    originalImage.getHeight)
}

case class BorderedResizedImage(val border: ImageBorder, val image: ResizedImage) {
  val width = 2 * border.width + image.width
  val height = 2 * border.width + image.height

  def size: RectangleSize = RectangleSize(width, height)

  def render: BufferedImage = {
    Preprocess.addBorder(border.width, border.color)(image.render)
  }

  def resize(newSize: RectangleSize): BorderedResizedImage = {
    val newWidth = newSize.width - 2 * border.width
    val newHeight = newSize.height - 2 * border.width
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
      finalSize.width - 2 * border.width,
      finalSize.height - 2 * border.width))

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