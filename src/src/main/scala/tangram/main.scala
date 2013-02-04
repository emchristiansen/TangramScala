package tangram

import org.jsoup._
import org.jsoup.nodes._
import javax.imageio.ImageIO
import java.net.URL
import com.frugalmechanic.optparse.OptParse
import com.frugalmechanic.optparse.MultiStrOpt
import com.frugalmechanic.optparse.StrOpt
import com.frugalmechanic.optparse.IntOpt
import java.io.File
import photostream.streams.StreamBing
import photostream.styles.BlockStyle
import photostream.streams.StreamFlickr

///////////////////////////////////////////////////////////

object Main extends OptParse {
  val archiveDirectoryOption = StrOpt()
  val imageStreamOption = StrOpt(
    default = Some("StreamFlickr.vibrant"))
  val styleOption = StrOpt(
    default = Some("BlockStyle"))
  val refreshDelayOption = IntOpt()

  def main(args: Array[String]) {
    parse(args)

//    def interpretString[A : Manifest](expression: String): A = {
//      val source = "import photostream.streams._; import photostream.styles._; val value: %s = %s; value".format(
//        implicitly[Manifest[A]],
//        expression)
//      (new Eval).apply[A](source)
//    }

    val imageStream = {
      // The image stream is JIT compiled based on the command-line argument.
      val unwrapped = Eval.eval[ImageStream](imageStreamOption.get)

      // This wraps the stream in such a way that getting the next
      // image also writes it to disk.
      unwrapped.imageStream.zipWithIndex.map({
        case (image, index) =>
          for (archiveDirectory <- archiveDirectoryOption) {
            val directory = new File(archiveDirectory)
            require(directory.isDirectory)
            val path = new File(directory, "image_%06d.png".format(index))
            ImageIO.write(image, "png", path)
          }

          image
      })
    }

    val style = Eval.eval[DisplayStyle](styleOption.get)

    val refreshDelay = refreshDelayOption.getOrElse(60)
    require(refreshDelay >= 0)

    val wallpaper = Wallpaper(Display.wallpaperWidth, Display.wallpaperHeight)

    Run.updateRunner(refreshDelay * 1000, style.updateWallpaper, wallpaper, imageStream)
  }
}

