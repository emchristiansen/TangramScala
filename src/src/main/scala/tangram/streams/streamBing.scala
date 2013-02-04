package tangram.streams

import java.awt.image.BufferedImage
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date

import org.jsoup.nodes.Element

import photostream.{ DocumentCacher, RetryingImageStream }

///////////////////////////////////////////////////////////

// TODO: This code could be prettier, but then Bing images suck
// so who cares.

// This is the Bing image of the day.
object StreamBing extends RetryingImageStream {
  def waitBetweenAttemptsInMs = 5 * 60 * 1000

  private val visitedDates = collection.mutable.Set[Date]()

  def nextImage: Option[BufferedImage] = {
    // This URL has links to (all?) the Bing images of the day.
    // Thanks to the following for the URL:
    // http://dgoins.wordpress.com/2011/06/27/changing-the-gnome-3-desktop-with-images-from-bing/
    val bingURL = new URL("http://themeserver.microsoft.com/default.aspx?p=Bing&c=Desktop&m=en-US")

    // Whenever we need a new image, we get the up-to-date list of images, and
    // return the newest one that we haven't already used.

    val doc = DocumentCacher.getDocument(bingURL)

    // Each URL is in an "item"
    val items = doc.getElementsByTag("item").toArray.toList.asInstanceOf[List[Element]]
    val datesAndURLs = for (item <- items) yield {
      val date = {
        val string = item.getElementsByTag("pubdate").toArray.head.asInstanceOf[Element].text
        val parser = new SimpleDateFormat("MM/dd/yyyy")
        parser.parse(string.takeWhile(_ != ' '))
      }

      val url = {
        val element = item.getElementsByTag("link").toArray.head.asInstanceOf[Element]
        val string = element.absUrl("ref")
        new URL(string.replace(" ", "%20"))
      }

      (date, url)
    }

    // We want the most recent image we haven't used.
    val newDatesAndURLS = datesAndURLs.sortBy(_._1).reverse.dropWhile(
      dateAndURL => visitedDates.contains(dateAndURL._1))

    if (newDatesAndURLS.isEmpty) return None

    val (date, url) = newDatesAndURLS.head

    // We mark this date as seen regardless of whether we can download the image.
    visitedDates += date

    val image = DocumentCacher.getImage(url)

    // If we downloaded the image, return it and a callback to this function for the rest
    // of the images. Else try again (call this function again).
    if (image != null) Some(image)
    else None
  }
}
    