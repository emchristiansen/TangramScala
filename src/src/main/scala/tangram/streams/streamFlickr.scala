package tangram.streams

import java.awt.image.BufferedImage
import java.net.URL
import org.jsoup.nodes.Element
import com.sun.syndication.feed.synd.SyndEntry
import com.sun.syndication.io.{ SyndFeedInput, XmlReader }
import tangram.stream.RetryingImageStream
import tangram._

///////////////////////////////////////////////////////////

case class StreamFlickr(url: URL)

object StreamFlickr {
  implicit class StreamFlickr2ImageFinder(
    self: StreamFlickr) extends ImageFinder {
    override def getImageURLs = (webCache: WebCache) => {
      val feedEntries = {
        println("Getting %s".format(self.url))
        val syndFeedInput = new SyndFeedInput
        val feed = syndFeedInput.build(new XmlReader(self.url))
        val uncast = feed.getEntries.toArray.toList
        uncast.asInstanceOf[List[SyndEntry]].sortBy(_.getPublishedDate).reverse
      }

      def getImageURL(
        entry: SyndEntry,
        webCache: WebCache): (Option[ImageURL], WebCache) = {
        // We need to visit the main page to get the full sized image.
        val link = new URL(entry.getLink)
        val imageAndCache = for (
          (mainPage, newWebCache) <- webCache.get(DocumentURL(link))
        ) yield {
          val allImages = {
            val uncast = mainPage.getElementsByTag("img").toArray.toList
            uncast.asInstanceOf[List[Element]]
          }

          // This is hacky and possibly brittle.
          val smallImageLink = {
            val filtered =
              allImages.filter(_.toString.contains("alt=\"photo\""))
            filtered.head.absUrl("src")
          }

          // The large image has the same name, with a different 
          // code at the end. Hacky.
          val largeImageURL =
            ImageURL(new URL(smallImageLink.replace("_z.jpg", "_b.jpg")))

          (largeImageURL, newWebCache)
        }

        imageAndCache match {
          case None => (None, webCache)
          case Some((imageURL, newWebCache)) =>
            imageURL.fetch match {
              case None => (None, newWebCache)
              case Some(image) =>
                // This is a hack to exclude the "Image doesn't exist" 
                // images that Flickr sometimes serves.
                if (image.getWidth != 500 || image.getHeight != 374)
                  (Some(imageURL), newWebCache)
                else (None, newWebCache)
            }
        }
      }

      feedEntries.foldLeft((List[ImageURL](), webCache)) {
        case ((imageURLs, webCache), entry) =>
          getImageURL(entry, webCache) match {
            case (None, newWebCache) => (imageURLs, newWebCache)
            case (Some(imageURL), newWebCache) =>
              (imageURL :: imageURLs, newWebCache)
          }
      }
    }
  }
}

//object StreamFlickr2 {
//  def fromStrings(urls: String*): StreamFlickr =
//    StreamFlickr(urls.toSeq.map(url => new URL(url)))
//
//  /////////////////////////////////////////////////////////
//
//  // Some streams gathered from
//  // http://www.pixiq.com/article/50-amazing-flickr-streams
//
//  def vibrant = fromStrings(
//    "http://api.flickr.com/services/feeds/photos_public.gne?id=49598046@N00&lang=en-us&format=rss_200",
//    "http://api.flickr.com/services/feeds/photos_public.gne?id=25052563@N08&lang=en-us&format=rss_200",
//    "http://api.flickr.com/services/feeds/photos_public.gne?id=66397474@N00&lang=en-us&format=rss_200")
//
//  def conceptual = fromStrings(
//    "http://api.flickr.com/services/feeds/photos_public.gne?id=83963013@N00&lang=en-us&format=rss_200",
//    "http://api.flickr.com/services/feeds/photos_public.gne?id=23790955@N06&lang=en-us&format=rss_200",
//    "http://api.flickr.com/services/feeds/photos_public.gne?id=14535004@N04&lang=en-us&format=rss_200",
//    "http://api.flickr.com/services/feeds/photos_public.gne?id=48848351@N00&lang=en-us&format=rss_200")
//}