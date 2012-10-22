package photostream

import org.jsoup._
import org.jsoup.nodes._

import javax.imageio.ImageIO

import java.net.URL

object Main extends App {
    val unusedImages = PhotoStream.getImages
//  val unusedImages = StreamBing.getImages

  val wallpaper = Wallpaper(Display.wallpaperWidth, Display.wallpaperHeight)

  //  Run.updateRunner(4000, RandomStrategy.single, wallpaper, unusedImages)

  Run.updateRunner(4000, Block.full, wallpaper, unusedImages)

}

