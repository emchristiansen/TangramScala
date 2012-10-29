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
import photostream.streams.StreamFlickr
import com.twitter.util.Eval

///////////////////////////////////////////////////////////

//  // Example with all available options instead of relying on the defaults
//  val message = BoolOpt(
//     long="message",          // Long name to use (--message)
//     short="m",               // Short name to use (-m)
//     default="Hello",         // Default value
//     desc="Message to print", // Help message description
//     enables=Nil,             // Other flags to enable if this one is enabled (single option or a Seq of options)
//     disables=Nil,            // Other flags to disable if this one is enabled (single option or a Seq of options)
//     invalidWith=Nil,         // Other options this flag is invalid with (they cannot be set)
//     validWith=Nil,           // Other options that are required with this flag
//     exclusive=false,         // Other options can be set when this option is set
//     validate="^[a-zA-Z ,]+$" // Use a regex for validation via an implicit that converts it to: (String) => Boolean
//  )

object Main extends OptParse {
  val archiveDirectoryOption = StrOpt()
  val imageStreamOption = StrOpt(
    default = Some("StreamFlickr.vibrant"))
  val styleOption = StrOpt(
    default = Some("Block"))
  val refreshDelayOption = IntOpt()

  def main(args: Array[String]) {
    parse(args)

    def interpretString[A](expression: String): A = {
      val source = "import photostream.streams._; import photostream.styles._; %s".format(
        expression)
      (new Eval).apply[A](source)
    }

    val imageStream = {
      // The image stream is JIT compiled based on the command-line argument.
      val unwrapped = interpretString[ImageStream](imageStreamOption.get)

      // This wraps the stream in such a way that getting the next
      // image also writes it to disk.
      unwrapped.imageStream.zipWithIndex.map({
        case (image, index) =>
          for (archiveDirectory <- archiveDirectoryOption) {
            val directory = new File(archiveDirectory)
            require(directory.isDirectory)
            val path = new File(directory, "image_%.6d.png".format(index))
            ImageIO.write(image, "png", path)
          }

          image
      })
    }

    val style = interpretString[DisplayStyle](styleOption.get)

    val refreshDelay = refreshDelayOption.getOrElse(60)
    require(refreshDelay >= 0)

    val wallpaper = Wallpaper(Display.wallpaperWidth, Display.wallpaperHeight)

    Run.updateRunner(refreshDelay * 1000, style.updateWallpaper, wallpaper, imageStream)
  }
}

