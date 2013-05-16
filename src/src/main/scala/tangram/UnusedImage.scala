package tangram

import nebula.imageProcessing._

////////////////////////////////////////

/**
 * An image that has not yet been used in a Tangram.
 * |numMisses| is the number of times the image has been passed over.
 */
case class UnusedImage(image: Image, numMisses: Int)

object UnusedImage {
  def apply(image: Image): UnusedImage = UnusedImage(image, 0)
  
  implicit def UnusedImage2Image(unusedImage: UnusedImage) = unusedImage.image
}