//import java.awt.image.BufferedImage
//import java.net.URL
//import org.scalatest.FunSuite
//import com.sun.syndication.io.{ SyndFeedInput, XmlReader }
//import tangram._
//import tangram.styles.BlockStyle
//import com.googlecode.flickrjandroid.Flickr
//import com.googlecode.flickrjandroid.REST
//import scala.runtime.RichInt
//import com.twitter.util.Eval
//
//import tangram.QuickImplicit._
//import tangram.Foo1._
//import tangram.Bar1._
//import tangram.Bar2._
//
/////////////////////////////////////////////////////////////
//
//class TestBlock extends FunSuite {
//  test("scratch") {
//    def eval[A: Manifest](expression: String) = {
//      val source = """
//      import nebula._;
//      import nebula.smallBaseline._;
//      import nebula.wideBaseline._;
//      import spark.SparkContext;
//      import SparkContext._;
//      
//      val value: %s = { %s };
//      value""".format(implicitly[Manifest[A]], expression)
//
//      (new Eval).apply[A](source)
//    }
//
//    val foo1 = Foo1()
//    
//    val c = (new Eval).apply[Int]("")
//    
////    val bar1 = foo1.runtimeTo[Bar1]
//    val bar2 = foo1.hiddenTo[Bar2]
//  }
//
//  ignore("make wallpaper from two images") {
//
//    val im0 = new BufferedImage(500, 500, BufferedImage.TYPE_BYTE_GRAY)
//    val im1 = new BufferedImage(500, 500, BufferedImage.TYPE_BYTE_GRAY)
//    val im2 = new BufferedImage(1000, 500, BufferedImage.TYPE_BYTE_GRAY)
//
//    val unusedImages = Seq(im0, im1, im2).toStream
//    val wallpaper = Wallpaper(1000, 1000)
//
//    //    Run.updateRunner(40000, Block.full, wallpaper, unusedImages)
//  }
//
//  ignore("blah") {
//    val sfi = new SyndFeedInput
//    val url = new URL("http://api.flickr.com/services/feeds/photos_public.gne?id=49598046@N00&lang=en-us&format=rss_200")
//    //    val url = new URL("http://www.google.com/reader/atom/feed/http://feeds.feedburner.com/bingimages?n=1000")
//    val feed = sfi.build(new XmlReader(url))
//    println(feed.getEntries.size)
//  }
//
//  ignore("flickr api") {
//    val apiKey = "3866899f6a37765e54a51430ac201867"
//    val apiSecret = "534e73ed854fc635"
//    val f = new Flickr(apiKey, apiSecret, new REST)
//
//    println(f.getPoolsInterface.getPhotos("Architecture", Array(), 100, 1))
//
//    //    f.getPoolsInterface.getPhotos(x$1, x$2, x$3, x$4)
//
//    val user = f.getPeopleInterface.findByUsername("heyoka")
//
//    println(user.getPhotosCount)
//
//    //    val photosetID = "http://www.flickr.com/photos/49598046@N00/sets/72157622991683276/"
//    //    val list = f.getPhotosetsInterface.getPhotos(photosetID, 100, 1)
//    //    println(list)
//
//    //    val testInterface = f.getTestInterface()
//    //    testInterface.e
//    //    val results = testInterface.echo(Collections.EMPTY_LIST)
//  }
//}
