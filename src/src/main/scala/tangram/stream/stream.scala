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
 * An image stream that sleeps then retries when it cannot get the next
 * image.
 */
trait RetryingImageStream extends ImageStream {
  def waitBetweenAttemptsInSeconds: Int
  
  def nextImage: Option[BufferedImage]
  
  /////////////////////////////////////////////////////////
  
  override def imageStream = {
    val imageOption = nextImage
    if (imageOption.isDefined) imageOption.get #:: imageStream
    else {
      println(s"No new images in stream, sleeping ${waitBetweenAttemptsInSeconds}s")
      Thread.sleep(waitBetweenAttemptsInSeconds * 1000)
      imageStream
    }
  }
}

