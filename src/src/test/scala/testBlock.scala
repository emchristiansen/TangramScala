import photostream._
import org.scalatest.FunSuite
import java.awt.image.BufferedImage
 
class TestBlock extends FunSuite {
  test("make wallpaper from two images") {
    
    val im0 = new BufferedImage(500, 500, BufferedImage.TYPE_BYTE_GRAY)
    val im1 = new BufferedImage(500, 500, BufferedImage.TYPE_BYTE_GRAY)
    val im2 = new BufferedImage(1000, 500, BufferedImage.TYPE_BYTE_GRAY)
    
    val unusedImages = Seq(im0, im1, im2).map(x => UnusedImage(x, 0)).toStream
    val wallpaper = Wallpaper(1000, 1000)
    
    Run.updateRunner(40000, Block.full, wallpaper, unusedImages)
  }
}
