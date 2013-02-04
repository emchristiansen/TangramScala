//package tangram
//
//import org.jsoup._
//import org.jsoup.nodes._
//import javax.imageio.ImageIO
//import java.net.URL
//import com.frugalmechanic.optparse.OptParse
//import com.frugalmechanic.optparse.MultiStrOpt
//import com.frugalmechanic.optparse.StrOpt
//import com.frugalmechanic.optparse.IntOpt
//import java.io.File
//import tangram.streams.StreamBing
//import tangram.styles.BlockStyle
//import tangram.streams.StreamFlickr
//import java.text.SimpleDateFormat
//import java.util.Date
//
/////////////////////////////////////////////////////////////
//
///**
// * The main class for Tangram.
// * Parses command-line arguments and starts the program.
// */
//object Main extends OptParse {
//  /**
//   * The filesystem path to the root of the src directory.
//   */  
//  val srcRoot: File = {
//    val resourceRoot = new File(getClass.getResource(".").getFile)
//    require(resourceRoot.exists)
//
//    // We assume a fixed structure relative to the resources directory.
//    new File(resourceRoot, "../../")
//  }
//
//  val archiveDirectoryOption = StrOpt(
//    desc = "An optional save directory for downloaded images. This directory must already exist.")
//  val imageStreamOption = StrOpt(
//    desc = "Path to the ImageStream Scala source code. The ImageStream controls which images will be fetched.",
//    default = Some(s"${srcRoot}/StreamFlickr.vibrant"))
//  val styleOption = StrOpt(
//    desc = "Path to the DisplayStyle Scala source code. The DisplayStyle controls how sets of images are arranged into a larger image.",
//    default = Some("BlockStyle"))
//  val refreshDelayOption = IntOpt(
//    desc = "Time in seconds each tangram will remain on the screen.",
//    default = Some(60))
//
//  def main(args: Array[String]) {
//    parse(args)
//
//    val now = new Date()
//
//    /**
//     * The ImageStream, which fetches images.
//     */
//    val imageStream = {
//      // The image stream is JIT compiled based on the command-line argument.
//      val unwrapped = Eval.eval[ImageStream](imageStreamOption.get)
//
//      // This wraps the stream in such a way that getting the next
//      // image also writes it to disk.
//      unwrapped.imageStream.zipWithIndex.map({
//        case (image, index) =>
//          for (archiveDirectory <- archiveDirectoryOption) {
//            // The archive directory must already exist.
//            val directory = new File(archiveDirectory)
//            require(directory.isDirectory)
//
//            // Create a new directory based on the current Date.
//            val dateFormat = new SimpleDateFormat("YYYY_MM_d_HH_mm_ss")
//            val subdirectory = new File(directory, dateFormat.format(now))
//            if (!subdirectory.exists && subdirectory.isDirectory)
//              subdirectory.mkdir()
//
//            // Number the downloaded images sequentially and save.
//            val path = new File(subdirectory, f"image_${index}%06d.png")
//            ImageIO.write(image, "png", path)
//          }
//
//          image
//      })
//    }
//
//    /**
//     * The DisplayStyle, which arranges sets of images into larger images.
//     */
//    val style = Eval.eval[DisplayStyle](styleOption.get)
//
//    /**
//     * The time in seconds each Tangram remains.
//     */
//    val refreshDelay = refreshDelayOption.get
//    require(refreshDelay >= 0)
//
//    /**
//     * The drawing canvas.
//     */
//    val wallpaper = Wallpaper(Display.wallpaperWidth, Display.wallpaperHeight)
//
//    // Runs the program.
//    Run.updateRunner(
//      refreshDelay * 1000,
//      style.updateWallpaper,
//      wallpaper,
//      imageStream)
//  }
//}
//
