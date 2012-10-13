package photostream

import Run.UpdateWallpaper
import java.awt.Color
import scala.util.Random

object RandomStrategy {
  val border = ImageBorder(2, Color.black)

  val random = new Random(0)

  def single: UpdateWallpaper = (wallpaper, images) => {
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