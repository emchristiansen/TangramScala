package tangram

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.File
import java.awt.Toolkit
import scala.annotation.tailrec
import tangram.style.DisplayStyle

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
  def setWallpaper[R <% Renderable](renderable: R) {
    require(renderable.size.width == wallpaperSize.width)
    require(renderable.size.height == wallpaperSize.height)
    
    val image = renderable.render
    ImageIO.write(image, "png", RuntimeUtil.wallpaperFile)

    // TODO: Make platform agnostic.
    def refreshWallpaper {
      val command =
        s"gsettings set org.gnome.desktop.background picture-uri file://${RuntimeUtil.wallpaperFile}"
      RuntimeUtil.runSystemCommand(command)
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
  def updateRunner[R <% Renderable, I <% ImageStream](
    minDelayInMilliseconds: Int,
    updateWallpaper: DisplayStyle.UpdateWallpaper[R, I],
    wallpaper: Wallpaper[R],
    imageStream: I) {

    @tailrec
    def refresh(
        wallpaper: Wallpaper[R], 
        images: I) {
      val (newWallpaper, newImages) = updateWallpaper(wallpaper, images)
      // The updater must do something.
      assert(newWallpaper != wallpaper)
      assert(newImages != images)
      DisplayUtil.setWallpaper(newWallpaper)
      println("sleeping for %dms".format(minDelayInMilliseconds))
      Thread.sleep(minDelayInMilliseconds)
      refresh(newWallpaper, newImages)
    }

    refresh(wallpaper, imageStream)
  }
}