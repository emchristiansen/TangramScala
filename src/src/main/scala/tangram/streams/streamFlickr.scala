package tangram.streams

import java.awt.image.BufferedImage
import java.net.URL
import org.jsoup.nodes.Element
import com.sun.syndication.feed.synd.SyndEntry
import com.sun.syndication.io.{ SyndFeedInput, XmlReader }
import tangram.stream.RetryingImageStream
import tangram._

///////////////////////////////////////////////////////////

case class StreamFlickr(url: URL, cache: WebCache, visitedURLs: Set[URL])

object StreamFlickr {
  
}

// TODO: This shares a lot of structure with StreamBing.
case class StreamFlickr2(urls: Seq[URL]) extends RetryingImageStream {
  override def waitBetweenAttemptsInSeconds = 5 * 60

  override def nextImage: Option[BufferedImage] = {
    // Whenever we need a new image, we get the up-to-date list of images, and
    // return the newest one that we haven't already used.    

    val allFeedEntries = (for (url <- urls) yield {
      println("Getting %s".format(url))
      val syndFeedInput = new SyndFeedInput
      val feed = syndFeedInput.build(new XmlReader(url))
      feed.getEntries.toArray.toList.asInstanceOf[List[SyndEntry]]
    }).flatten

    val newEntries = allFeedEntries.filterNot(oldEntries)

    if (newEntries.isEmpty) return None

    // We take the newest entry.
    val entry = newEntries.maxBy(_.getPublishedDate)
    oldEntries += entry

    try {
      // We need to visit the main page to get the full sized image.
      val link = new URL(entry.getLink)
      val mainPage = DocumentCacher.getDocument(link)
      val allImages = mainPage.getElementsByTag("img").toArray.toList.asInstanceOf[List[Element]]

      // This is hacky and possibly brittle.
      val smallImageLink = allImages.filter(_.toString.contains("alt=\"photo\"")).head.absUrl("src")

      // The large image has the same name, with a different code at the end. Hacky.
      val largeImageLink = new URL(smallImageLink.replace("_z.jpg", "_b.jpg"))

      val image = (DocumentCacher.getImage(largeImageLink))
      assert(image != null)
      
      // This is a hack to exclude the "Image doesn't exist" images that
      // Flickr sometimes serves.
      assert(image.getWidth != 500 || image.getHeight != 374)
      
      Some(image)
    } catch {
      case _: Throwable => {
        println("Failed to fetch image for entry %s".format(entry.getTitle))
        nextImage
      }
    }
  }
}

object StreamFlickr2 {
  def fromStrings(urls: String*): StreamFlickr =
    StreamFlickr(urls.toSeq.map(url => new URL(url)))

  /////////////////////////////////////////////////////////

  // Some streams gathered from
  // http://www.pixiq.com/article/50-amazing-flickr-streams

  def vibrant = fromStrings(
    "http://api.flickr.com/services/feeds/photos_public.gne?id=49598046@N00&lang=en-us&format=rss_200",
    "http://api.flickr.com/services/feeds/photos_public.gne?id=25052563@N08&lang=en-us&format=rss_200",
    "http://api.flickr.com/services/feeds/photos_public.gne?id=66397474@N00&lang=en-us&format=rss_200")

  def conceptual = fromStrings(
    "http://api.flickr.com/services/feeds/photos_public.gne?id=83963013@N00&lang=en-us&format=rss_200",
    "http://api.flickr.com/services/feeds/photos_public.gne?id=23790955@N06&lang=en-us&format=rss_200",
    "http://api.flickr.com/services/feeds/photos_public.gne?id=14535004@N04&lang=en-us&format=rss_200",
    "http://api.flickr.com/services/feeds/photos_public.gne?id=48848351@N00&lang=en-us&format=rss_200")
}