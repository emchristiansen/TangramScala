package tangram

import java.awt.Color
import java.awt.image.BufferedImage

///////////////////////////////////////////////////////////

/**
 * Represents a border painted around an image for aesthetic effects.
 */
case class ImageBorder(width: Int, color: Color) {
  require(width >= 0)
}

/**
 * A Renderable with a border.
 */
case class BorderedRenderable[R <% Renderable](
  border: ImageBorder,
  renderable: R)

object BorderedRenderable {
  /**
   * Adds the specified ImageBorder to an image.
   */
  def addBorder(
    border: ImageBorder,
    image: BufferedImage): BufferedImage = {
    val newWidth = image.getWidth + 2 * border.width
    val newHeight = image.getHeight + 2 * border.width
    val newImage = new BufferedImage(
      newWidth,
      newHeight,
      BufferedImage.TYPE_INT_ARGB)
    val graphics = newImage.createGraphics
    graphics.setColor(border.color)
    graphics.fillRect(0, 0, newWidth, newHeight)
    graphics.drawImage(image, border.width, border.width, null)
    newImage
  }

  implicit class BorderedRenderable2Renderable[R <% Renderable](
    self: BorderedRenderable[R]) extends Renderable {
    override def render = {
      val noBorder = self.renderable.render
      addBorder(self.border, noBorder)
    }
  }

  //  val width = 2 * border.width + image.width
  //  val height = 2 * border.width + image.height
  //
  //  def size: RectangleSize = RectangleSize(width, height)
  //
  //  def render: BufferedImage = {
  //    Preprocess.addBorder(border.width, border.color)(image.render)
  //  }
  //
  //  def resize(newSize: RectangleSize): BorderedResizedImage = {
  //    val newWidth = newSize.width - 2 * border.width
  //    val newHeight = newSize.height - 2 * border.width
  //    copy(image = image.copy(width = newWidth, height = newHeight))
  //  }
}

//object BorderedResizedImage {
//  def resizeToFit(
//    border: ImageBorder,
//    finalSize: RectangleSize,
//    image: BufferedImage): BorderedResizedImage = BorderedResizedImage(
//    border,
//    ResizedImage(
//      image,
//      finalSize.width - 2 * border.width,
//      finalSize.height - 2 * border.width))
//
//  implicit def implicitRectangleLike(self: BorderedResizedImage): RectangleLike = new RectangleLike {
//    override def size = self.size
//  }
//}
//
//object Preprocess {
//
//}