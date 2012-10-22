package photostream

import org.jsoup._
import org.jsoup.nodes._
import javax.imageio.ImageIO
import java.net.URL
import com.frugalmechanic.optparse.OptParse
import com.frugalmechanic.optparse.MultiStrOpt
import com.frugalmechanic.optparse.StrOpt
import com.frugalmechanic.optparse.IntOpt
import java.io.File

///////////////////////////////////////////////////////////

object Main extends OptParse {
  val imageSaveDirectoryOption = StrOpt()
  val photoStreamOption = StrOpt()
  val layoutOption = StrOpt()
  val refreshDelayOption = IntOpt()

  def main(args: Array[String]) {
    parse(args)

    val photoStream = {
      val unwrapped = photoStreamOption.getOrElse("bing") match {
        case "bing" => StreamBing.getImages
        case _ => sys.error("photo stream not recognized")
      }
      
      // This wraps the stream in such a way that getting the next
      // image also writes it to disk.
      unwrapped.zipWithIndex.map({case (image, index) =>
        for (imageSaveDirectory <- imageSaveDirectoryOption) {
          val directory = new File(imageSaveDirectory)
          require(directory.isDirectory)
          val path = new File(directory, "image_%.6d.png".format(index))
          ImageIO.write(image.image, "png", path)
        }
        
        image
      })
    }

    val layout = layoutOption.getOrElse("block") match {
      case "block" => Block.full
      case _ => sys.error("layout not recognized")
    }

    val refreshDelay = refreshDelayOption.getOrElse(60)
    require(refreshDelay >= 0)

    val wallpaper = Wallpaper(Display.wallpaperWidth, Display.wallpaperHeight)

    Run.updateRunner(refreshDelay * 1000, Block.full, wallpaper, photoStream)
  }
}

