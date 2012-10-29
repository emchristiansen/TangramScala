import photostream._
import org.scalatest.FunSuite
import java.awt.image.BufferedImage
import com.sun.syndication.io.SyndFeedInput
import java.net.URL
import com.sun.syndication.io.XmlReader
import com.sun.syndication.feed.synd.SyndEntry
import photostream.styles.Block
 
import org.jsoup._
import org.jsoup.nodes._
import javax.imageio.ImageIO
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.awt.image.BufferedImage
import photostream.UnusedImage
import scala.collection.immutable.Stream.consWrapper

class TestBlock extends FunSuite {
  ignore("make wallpaper from two images") {
    
    val im0 = new BufferedImage(500, 500, BufferedImage.TYPE_BYTE_GRAY)
    val im1 = new BufferedImage(500, 500, BufferedImage.TYPE_BYTE_GRAY)
    val im2 = new BufferedImage(1000, 500, BufferedImage.TYPE_BYTE_GRAY)
    
    val unusedImages = Seq(im0, im1, im2).map(x => UnusedImage(x, 0)).toStream
    val wallpaper = Wallpaper(1000, 1000)
    
    Run.updateRunner(40000, Block.full, wallpaper, unusedImages)
  }
  
  test("blah") {
    val sfi = new SyndFeedInput
    val url = new URL("http://feeds.feedburner.com/bingimages")
//    val url = new URL("http://www.google.com/reader/atom/feed/http://feeds.feedburner.com/bingimages?n=1000")
    val feed = sfi.build(new XmlReader(url))
    println(feed.getEntries.size)
  }
}
