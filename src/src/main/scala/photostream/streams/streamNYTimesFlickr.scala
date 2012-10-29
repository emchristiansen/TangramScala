package photostream.streams

import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import scala.collection.immutable.Stream.consWrapper
import org.jsoup.nodes.Element
import photostream.{ DocumentCacher, UnusedImage }
import com.sun.syndication.io.SyndFeedInput
import org.xml.sax.XMLReader
import com.sun.syndication.io.XmlReader
import com.sun.syndication.feed.synd.SyndEntry

///////////////////////////////////////////////////////////

// This is the NYTimes daily image Flickr stream.
// TODO: This shares a lot of structure with StreamBing.
object StreamNYTimesFlickr {
    val nyTimesURL = new URL("http://api.flickr.com/services/feeds/photos_public.gne?id=49598046@N00&lang=en-us&format=rss_200")
//  val nyTimesURL = new URL("http://api.flickr.com/services/feeds/photos_public.gne?id=40016081@N04&lang=en-us&format=rss_200")

  private val visitedDates = collection.mutable.Set[Date]()

  def getImages: Stream[UnusedImage] = {
    val feedEntries = {
      val syndFeedInput = new SyndFeedInput
      val feed = syndFeedInput.build(new XmlReader(nyTimesURL))
      feed.getEntries.toArray.toList.asInstanceOf[List[SyndEntry]]
    }

    val datesAndURLs = for (entry <- feedEntries) yield {
      // We need to visit the main page to get the full sized image.
      val link = new URL(entry.getLink)
      val mainPage = DocumentCacher.getDocument(link)
      val allImages = mainPage.getElementsByTag("img").toArray.toList.asInstanceOf[List[Element]]

      // This is hacky and possibly brittle.
      val smallImageLink = allImages.filter(_.toString.contains("alt=\"photo\"")).head.absUrl("src")

      // The large image has the same name, with a different code at the end. Hacky.
      val largeImageLink = new URL(smallImageLink.replace("_z.jpg", "_b.jpg"))

      (entry.getPublishedDate, largeImageLink)
    }

    // We want the most recent image we haven't used.
    val (date, url) = datesAndURLs.sortBy(_._1).reverse.dropWhile(
      dateAndURL => visitedDates.contains(dateAndURL._1)).headOption.getOrElse(
        sys.error("It appears we already used all %d images".format(datesAndURLs.size)))

    // We mark this date as seen regardless of whether we can download the image.
    visitedDates += date

    val image = DocumentCacher.getImage(url)

    // If we downloaded the image, return it and a callback to this function for the rest
    // of the images. Else try again (call this function again).
    if (image != null) UnusedImage(image, 0) #:: getImages
    else getImages
  }
}