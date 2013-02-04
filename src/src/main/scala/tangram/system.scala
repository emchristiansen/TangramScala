package tangram

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.File
import java.awt.Toolkit
import scala.annotation.tailrec

import styles._

///////////////////////////////////////////////////////////

object Display {
  // TODO: Make this reflect the size of the desktop, which is actually smaller
  // than the screen resolution, owing to possible bars at the top or bottom.
  val (wallpaperWidth, wallpaperHeight) = {
    val screenSize = Toolkit.getDefaultToolkit.getScreenSize
    (screenSize.width, screenSize.height)
  }

  def refreshWallpaper {
    val command = "gsettings set org.gnome.desktop.background picture-uri file://%s".format(Runtime.wallpaperFile)
    Runtime.runSystemCommand(command)
  }

  def setWallpaper(image: BufferedImage) {
    require(image.getWidth == wallpaperWidth)
    require(image.getHeight == wallpaperHeight)
    ImageIO.write(image, "png", Runtime.wallpaperFile)

    // TODO
    refreshWallpaper
  }
}

object Runtime {
  // TODO: Make system agnostic
  val wallpaperFile = new File("/tmp/photostream_wallpaper.png")

  def runSystemCommand(command: String): String = {
    println("running system command: %s".format(command))
    try {
      val out = sys.process.Process(command).!!
      println("successfully ran system command")
      out
    } catch {
      case e: Exception => throw new Exception("system command failed: %s\nException was %s\n".format(command, e.toString))
    }
  }
}

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