package tangram.styles

import photostream._
import java.awt.Color
import scala.util.Random
import photostream.UnusedImage

///////////////////////////////////////////////////////////

object RandomStyle extends DisplayStyle {
  val border = ImageBorder(1, Color.white)

  val random = new Random(0)

  def updateWallpaper = (wallpaper, images) => {
    val UnusedImage(headImage, _) #:: tailImages = images
    
    val (x, y) = (
      random.nextInt(wallpaper.width - headImage.getWidth), 
      random.nextInt(wallpaper.height - headImage.getHeight))    
    
    val newWallpaper = wallpaper.insert(
      BorderedResizedImage(
        border,
        ResizedImage(headImage, headImage.getWidth, headImage.getHeight)),
      Position(x, y))
      
    (newWallpaper, tailImages)
  }
}