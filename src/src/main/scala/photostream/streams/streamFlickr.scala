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

// TODO: This shares a lot of structure with StreamBing.
object StreamFlickr {
  private val oldEntries = collection.mutable.Set[SyndEntry]()

  def getImages(urls: Seq[URL]): Stream[UnusedImage] = {
    val allFeedEntries = (for (url <- urls) yield {
      val syndFeedInput = new SyndFeedInput
      val feed = syndFeedInput.build(new XmlReader(url))
      feed.getEntries.toArray.toList.asInstanceOf[List[SyndEntry]]
    }).flatten

    val newEntries = allFeedEntries.filterNot(oldEntries)
    
    if (newEntries.isEmpty) 
      sys.error("It appears we already used all %d images".format(allFeedEntries.size))

    // We take the newest entry.
    val entry = newEntries.maxBy(_.getPublishedDate)
    oldEntries += entry

    // We need to visit the main page to get the full sized image.
    val link = new URL(entry.getLink)
    val mainPage = DocumentCacher.getDocument(link)
    val allImages = mainPage.getElementsByTag("img").toArray.toList.asInstanceOf[List[Element]]

    // This is hacky and possibly brittle.
    val smallImageLink = allImages.filter(_.toString.contains("alt=\"photo\"")).head.absUrl("src")

    // The large image has the same name, with a different code at the end. Hacky.
    val largeImageLink = new URL(smallImageLink.replace("_z.jpg", "_b.jpg"))
    
    val image = DocumentCacher.getImage(largeImageLink)

    // If we downloaded the image, return it and a callback to this function for the rest
    // of the images. Else try again (call this function again).
    if (image != null) UnusedImage(image, 0) #:: getImages(urls)
    else getImages(urls)
  }

}