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
import photostream.streams.StreamBing
import photostream.styles.Block
import photostream.streams.StreamNYTimesFlickr
import photostream.streams.StreamFlickr

///////////////////////////////////////////////////////////

object Main extends OptParse {
  val imageSaveDirectoryOption = StrOpt()
  val photoStreamOption = StrOpt()
  val styleOption = StrOpt()
  val refreshDelayOption = IntOpt()

  def main(args: Array[String]) {
    parse(args)

    val flickrURLs = Seq(
      "http://api.flickr.com/services/feeds/photos_public.gne?id=15171232@N02&lang=en-us&format=rss_200",
      "http://api.flickr.com/services/feeds/photos_public.gne?id=49598046@N00&lang=en-us&format=rss_200",
      "http://api.flickr.com/services/feeds/photos_public.gne?id=14686714@N00&lang=en-us&format=rss_200")

    val photoStream = {
      val unwrapped = photoStreamOption.getOrElse("bing") match {
        case "bing" => StreamBing.getImages
        case "nytimes" => StreamNYTimesFlickr.getImages
        case "flickr" => StreamFlickr.getImages(flickrURLs.map(url => new URL(url)))
        case _ => sys.error("photo stream not recognized")
      }

      // This wraps the stream in such a way that getting the next
      // image also writes it to disk.
      unwrapped.zipWithIndex.map({
        case (image, index) =>
          for (imageSaveDirectory <- imageSaveDirectoryOption) {
            val directory = new File(imageSaveDirectory)
            require(directory.isDirectory)
            val path = new File(directory, "image_%.6d.png".format(index))
            ImageIO.write(image.image, "png", path)
          }

          image
      })
    }

    val style = styleOption.getOrElse("block") match {
      case "block" => Block.full
      case _ => sys.error("layout not recognized")
    }

    val refreshDelay = refreshDelayOption.getOrElse(60)
    require(refreshDelay >= 0)

    val wallpaper = Wallpaper(Display.wallpaperWidth, Display.wallpaperHeight)

    Run.updateRunner(refreshDelay * 1000, Block.full, wallpaper, photoStream)
  }
}

