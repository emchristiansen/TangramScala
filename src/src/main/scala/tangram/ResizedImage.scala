package tangram

import java.awt.Color
import java.awt.image.BufferedImage
import org.imgscalr.Scalr

///////////////////////////////////////////////////////////

/**
 * An unresized image along with a target size.
 */
case class ResizedImage(
  originalImage: BufferedImage,
  size: RectangleSize) {
  require(originalImage.getWidth > 0)
  require(originalImage.getHeight > 0)
}

object ResizedImage {
  /**
   * A trivial ResizedImage with the target size the same as the
   * original size.
   */
  def apply(originalImage: BufferedImage): ResizedImage = ResizedImage(
    originalImage,
    RectangleSize(originalImage.getWidth, originalImage.getHeight))

  implicit class ResizedImage2Renderable(
      self: ResizedImage) extends Renderable {
    /**
     * Resize the original image to have the target size, and return the
     * result as a BufferedImage.
     * This method achieves the desired aspect ratio by cropping, not
     * warping.
     */
    override def render = {
      /**
       * Rescale an image by the desired factor.
       */
      def scale(
        scaleFactor: Double,
        image: BufferedImage): BufferedImage = {
        if (scaleFactor == 1) image
        else {
          val scaledWidth = (scaleFactor * image.getWidth).round.toInt
          val scaledHeight = (scaleFactor * image.getHeight).round.toInt

          // Rescaling using the default Graphics canvas produces ugly results,
          // so we use this nifty package.
          Scalr.resize(
            image,
            Scalr.Method.ULTRA_QUALITY,
            Scalr.Mode.FIT_EXACT,
            scaledWidth,
            scaledHeight)
        }
      }

      val originalSize = RectangleSize(
        originalImage.getWidth,
        originalImage.getHeight)

      val scaleFactor = if (originalSize.aspect > self.size.aspect) {
        // Make the heights match.
        self.size.height.toDouble / originalSize.height
      } else {
        // Make the widths match.
        self.size.width.toDouble / originalSize.width
      }

      val scaled = scale(originalImage, scaleFactor)

      // Crop off the extra bits.
      val extraWidth = scaled.getWidth - self.size.width
      val extraHeight = scaled.getHeight - self.size.height
      scaled.getSubimage(
        extraWidth / 2,
        extraHeight / 2,
        self.size.width,
        self.size.height)
    }
  }
}

