//package tangram.streams
//
//import java.awt.image.BufferedImage
//import java.net.URL
//import java.text.SimpleDateFormat
//import java.util.Date
//import org.jsoup.nodes.Element
//import tangram._
//import tangram.stream.RetryingImageStream
//import org.jsoup.nodes.Document
//import tangram.stream.RetryingImageStream
//
/////////////////////////////////////////////////////////////
//
///**
// * Used to stream the Bing image of the day.
// */
//case class StreamBing()
//
//object StreamBing {
//  implicit class StreamBing2ImageFinder(
//    self: StreamBing) extends ImageFinder {
//    override def getImageURLs = (webCache: WebCache) => {
//      // This URL has links to (all?) the Bing images of the day.
//      // Thanks to the following for the URL:
//      // http://dgoins.wordpress.com/2011/06/27/changing-the-gnome-3-desktop-with-images-from-bing/
//      val bingURL = new URL(
//        "http://themeserver.microsoft.com/default.aspx?p=Bing&c=Desktop&m=en-US")
//
//      // Whenever we need a new image, we get the up-to-date list of images, 
//      // and return the newest one that we haven't already used.
//      webCache.get(DocumentURL(bingURL)) match {
//        // We could not fetch any image URLs.
//        case None => (Nil, webCache)
//        // We did fetch image URLs.
//        case Some((doc, newWebCache)) => {
//          // Each URL is in an "item"
//          val items = {
//            val uncast = doc.getElementsByTag("item").toArray.toList
//            uncast.asInstanceOf[List[Element]]
//          }
//          val datesAndURLs = (for (item <- items) yield {
//            val date = {
//              val string = {
//                val uncast = item.getElementsByTag("pubdate").toArray.head
//                uncast.asInstanceOf[Element].text
//              }
//              val parser = new SimpleDateFormat("MM/dd/yyyy")
//              parser.parse(string.takeWhile(_ != ' '))
//            }
//
//            val url = {
//              val element = {
//                val uncast = item.getElementsByTag("link").toArray.head
//                uncast.asInstanceOf[Element]
//              }
//              val string = element.absUrl("ref")
//              new URL(string.replace(" ", "%20"))
//            }
//
//            (date, ImageURL(url))
//          })
//
//          (datesAndURLs.sortBy(_._1).reverse.map(_._2), newWebCache)
//        }
//      }
//    }
//  }
//}
//    