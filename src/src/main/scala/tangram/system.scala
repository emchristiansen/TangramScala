package tangram

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.File
import java.awt.Toolkit
import scala.annotation.tailrec

import styles._

///////////////////////////////////////////////////////////

/**
 * Tools for setting wallpaper images.
 */
object DisplayUtil {
  /**
   * The appropriate size of the wallpaper.
   */
  // TODO: Make this reflect the size of the desktop, which is actually smaller
  // than the screen resolution, owing to possible bars at the top or bottom.
  val wallpaperSize = {
    val screenSize = Toolkit.getDefaultToolkit.getScreenSize
    RectangleSize(screenSize.width, screenSize.height)
  }

  /**
   * Sets a given image as the wallpaper.
   */
  def setWallpaper(image: BufferedImage) {
    require(image.getWidth == wallpaperSize.width)
    require(image.getHeight == wallpaperSize.height)
    ImageIO.write(image, "png", Runtime.wallpaperFile)

    // TODO: Make platform agnostic.
    def refreshWallpaper {
      val command =
        s"gsettings set org.gnome.desktop.background picture-uri file://${Runtime.wallpaperFile}"
      Runtime.runSystemCommand(command)
    }

    refreshWallpaper
  }
}

/**
 * Tools for interacting with the system.
 */
object RuntimeUtil {
  val homeDirectory = new File(System.getProperty("user.home"))
  
  /**
   * The Tangram directory, located in ~/.tangram.
   * Automatically created if it doesn't already exist.
   */
  val tangramDirectory = {
    val directory = new File(homeDirectory, ".tangram")
    if (!directory.exists || !directory.isDirectory)
      directory.mkdir
    directory
  }

  /**
   * Where the final wallpaper is saved to be used by the system.
   */
  val wallpaperFile = {
    new File(tangramDirectory, "wallpaper.png")
  }

  def runSystemCommand(command: String): String = {
    println(s"Running system command: ${command}")
    try {
      val out = sys.process.Process(command).!!
      println("Successfully ran system command")
      out
    } catch {
      case e: Exception => 
        throw new Exception(s"System command failed: ${command}\nException was ${e}\n")
    }
  }
}

/**
 * Contains the main loop used to update the wallpaper.
 */
// TODO: Rename
object Run {
  def updateRunner(
    minDelayInMilliseconds: Int,
    updateWallpaper: DisplayStyle.UpdateWallpaper,
    wallpaper: Wallpaper,
    imageStream: Stream[BufferedImage]) {

    @tailrec
    def refresh(wallpaper: Wallpaper, images: Stream[UnusedImage]) {
      val (newWallpaper, newImages) = updateWallpaper(wallpaper, images)
      // The updater must do something.
      assert(newWallpaper != wallpaper)
      assert(newImages != images)
      Display.setWallpaper(newWallpaper.render)
      println("sleeping for %dms".format(minDelayInMilliseconds))
      Thread.sleep(minDelayInMilliseconds)
      refresh(newWallpaper, newImages)
    }

    refresh(wallpaper, imageStream.map(UnusedImage.apply))
  }
}