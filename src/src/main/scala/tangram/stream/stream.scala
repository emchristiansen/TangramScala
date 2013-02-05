package tangram.stream

import java.awt.image.BufferedImage
import scala.collection.immutable.Stream.consWrapper

///////////////////////////////////////////////////////////

/**
 * An image that has not yet been used in a Tangram.
 * |numMisses| is the number of times the image has been passed over.
 */
case class UnusedImage(image: BufferedImage, numMisses: Int)

object UnusedImage {
  def apply(image: BufferedImage): UnusedImage = UnusedImage(image, 0)
}

/**
 * A provider of a potentially unlimited number of images.
 */
trait ImageStream {
  def imageStream: Stream[BufferedImage]
}

object ImageStream {
  //  // TODO: implicit  
  //  def isImageFile(file: File): Boolean = {
  //    val extensions = Seq(".png", ".jpg", ".jpeg", ".bmp")
  //    extensions.map(extension => file.toString.endsWith(extension)).contains(true)
  //  }
  //  
  //  def getImages: Stream[UnusedImage] = {
  //    val imageDirectory = new File("/u/echristiansen/Dropbox/scala/12Summer/tangram.data")
  //    assert(imageDirectory.exists)
  //    val files = imageDirectory.listFiles.filter(isImageFile)
  //    files.toStream.map(UnusedImage.apply)
  //  }
}

/**
 * An image stream that might not sometimes fail to provide an image.
 */
trait FailingImageStream extends ImageStream {
  def failingImageStream: Stream[Option[BufferedImage]]

  /////////////////////////////////////////////////////////

  override def imageStream = failingImageStream.flatten
}

/**
 * An image stream that sleeps then retries when it cannot get the next
 * image.
 */
trait RetryingImageStream extends FailingImageStream {
  def waitBetweenAttemptsInSeconds: Int
  
  /**
   * An image stream that does not sleep between retries.
   */
  // TODO: This can probably be turned into a mixin.
  def noWaitingImageStream: Stream[Option[BufferedImage]]

  /////////////////////////////////////////////////////////  
  
  override def failingImageStream = noWaitingImageStream map {
    // If the image was successfully retrieved, we insert no
    // delay.
    case Some(image) => Some(image)    
    
    // If the image was not retrieved, we sleep before returning None.
    case None => {
      println(s"No new images in stream, sleeping ${waitBetweenAttemptsInSeconds}s")
      Thread.sleep(waitBetweenAttemptsInSeconds * 1000)
      None
    }
  }
}

