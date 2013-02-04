package tangram

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.File

///////////////////////////////////////////////////////////

case class UnusedImage(image: BufferedImage, numMisses: Int)

object UnusedImage {
//  def apply(file: File): UnusedImage = UnusedImage(ImageIO.read(file), 0)
  
  def apply(image: BufferedImage): UnusedImage = UnusedImage(image, 0)
}

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

trait RetryingImageStream extends ImageStream {
  def waitBetweenAttemptsInMs: Int
  
  def nextImage: Option[BufferedImage]
  
  override def imageStream = {
    val imageOption = nextImage
    if (imageOption.isDefined) imageOption.get #:: imageStream
    else {
      println("No new images in stream, sleeping %sms".format(waitBetweenAttemptsInMs))
      Thread.sleep(waitBetweenAttemptsInMs)
      imageStream
    }
  }
}

