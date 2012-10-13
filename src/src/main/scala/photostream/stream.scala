package photostream

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.File

case class UnusedImage(image: BufferedImage, numMisses: Int)

object UnusedImage {
  def apply(file: File): UnusedImage = UnusedImage(ImageIO.read(file), 0)
}

object PhotoStream {
  def isImageFile(file: File): Boolean = {
    val extensions = Seq(".png", ".jpg", ".jpeg", ".bmp")
    extensions.map(extension => file.toString.endsWith(extension)).contains(true)
  }

  def getImages: Stream[UnusedImage] = {
    val imageDirectory = new File("/u/echristiansen/Dropbox/scala/12Summer/photostream/data")
    assert(imageDirectory.exists)
    val files = imageDirectory.listFiles.filter(isImageFile)
    files.toStream.map(UnusedImage.apply)
  }
}