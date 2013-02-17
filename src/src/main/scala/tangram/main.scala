package tangram

import com.frugalmechanic.optparse.StrOpt
import com.frugalmechanic.optparse.IntOpt
import com.frugalmechanic.optparse.OptParse
import tangram.stream.ImageStream
import tangram.style.DisplayStyle
import java.io.File
import java.util.Date
import javax.imageio.ImageIO
import java.text.SimpleDateFormat
import org.rogach.scallop._
import org.apache.commons.io.FileUtils
import tangram.Eval._

///////////////////////////////////////////////////////////

class Conf(args: Seq[String]) extends ScallopConf(args) {
  banner("Tangram: A program for automatically assembling mosaics from images found on the web and setting them as the desktop wallpaper.")

  val imageStream = opt[String](
    descr = "Scale expression with implicit view to ImageStream. The ImageStream controls which images will be fetched.",
    default = Some("StreamFlickr.vibrant"))

  val style = opt[String](
    descr = "Scale expression with implicit view to DisplayStyle. The DisplayStyle controls how sets of images are arranged into a larger image.",
    default = Some("BlockStyle"))

  val extraSourceFiles = opt[List[String]](
    descr = "Extra Scala source files to compile against at runtime. Use this to add additional behavior without modifying the core Tangram source. To ease debugging, each file must independently be valid Scala source.",
    required = false,
    default = Some(Nil)) map (_ map ExistingFile.apply)

  val refreshDelay = opt[Int](
    descr = "Time in seconds each tangram will remain on the screen.",
    default = Some(60))

  val archiveDirectory = opt[String](
    descr = "An optional save directory for downloaded images. This directory must already exist.",
    required = false) map ExistingDirectory.apply
}

/**
 * Holds files that exist and are actual files (not directories).
 */
case class ExistingFile(file: File) {
  require(file.isFile, s"${file} is not an existing file.")
}

object ExistingFile {
  def apply(filename: String): ExistingFile = ExistingFile(new File(filename))

  implicit def existingFile2File(self: ExistingFile) =
    self.file
}

/**
 * Holds directories that exist and are actual directories (not files).
 */
case class ExistingDirectory(directory: File) {
  require(directory.isFile, s"${directory} is not an existing directory.")
}

object ExistingDirectory {
  def apply(filename: String): ExistingDirectory =
    ExistingDirectory(new File(filename))

  implicit def existingDirectory2File(self: ExistingDirectory) =
    self.directory
}

object Main {
  def validate(args: Conf) {
    // Make sure each extra source file is independently valid Scala code.
    for (file <- args.extraSourceFiles.get.get) {
      val source = FileUtils.readFileToString(file)
      require(Eval.hasType[Any](source), s"${file} is not valid Scala source.")
    }

    // Make sure the ImageStream expression has an implicit view to ImageStream. 
    val imageStreamSource =
      args.imageStream.get.get.addImports.include(args.extraSourceFiles.get.get)
    require(
      Eval.hasType[ImageStream](imageStreamSource),
      s"${args.imageStream} does not have an implicit view to ImageStream.")

    // Make sure the DisplayStyle expresssion has an implicit view
    // to DisplayStyle.
    val displayStyleSource =
      args.style.get.get.addImports.include(args.extraSourceFiles.get.get)
    require(
      Eval.hasType[DisplayStyle[_, _]](displayStyleSource),
      s"${args.style} does not have an implicit view to DisplayStyle.")
  }

  def main(unparsedArgs: Array[String]) {
    val args = new Conf(unparsedArgs)
    println(args.summary)
    validate(args)
  }
}

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
////    /**
////     * The DisplayStyle, which arranges sets of images into larger images.
////     */
////    val style = Eval.eval[DisplayStyle](styleOption.get)
////
////    /**
////     * The time in seconds each Tangram remains.
////     */
////    val refreshDelay = refreshDelayOption.get
////    require(refreshDelay >= 0)
////
////    /**
////     * The drawing canvas.
////     */
////    val wallpaper = Wallpaper(Display.wallpaperWidth, Display.wallpaperHeight)
////
////    // Runs the program.
////    Run.updateRunner(
////      refreshDelay * 1000,
////      style.updateWallpaper,
////      wallpaper,
////      imageStream)
//  }
//}

