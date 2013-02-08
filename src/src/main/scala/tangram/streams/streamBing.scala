package tangram.streams

import java.awt.image.BufferedImage
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import org.jsoup.nodes.Element
import tangram._
import tangram.stream.RetryingImageStream
import org.jsoup.nodes.Document
import tangram.stream.RetryingImageStream

///////////////////////////////////////////////////////////

/**
 * Used to stream the Bing image of the day.
 */
case class StreamBing(cache: WebCache, visitedURLs: Set[URL])

trait TwoPassStream extends RetryingImageStream {
  def getImageURLs: (Option[Set[ImageURL]], Set[ImageURL], Set[ImageURL] => TwoPassStream)

  ///////////////////////////////////////////////////////////

  override def noWaitingStream: Stream[Option[BufferedImage]] =
    getImageURLs match {
      // We couldn't fetch any image URLs.
      case (None, _, stream) => None #:: stream(Set()).noWaitingStream
      // We successfully fetched new image URLS.
      case (Some(imageURLs), visitedURLs, stream) =>
        imageURLs.filter({
          // Remove all previously seen image URLs.
          imageURL => !visitedURLs.contains(imageURL)
        }).headOption match {
          // We have seen all these image URLs.
          case None =>
            None #:: None #:: stream(Set()).noWaitingStream
          // There's a new URL.
          case Some(imageURL @ ImageURL(url)) => {
            // We mark this url as seen regardless of whether we can 
            // download the image.
            val newVisitedURLs = visitedURLs + imageURL

            // We don't cache the downloaded image, since we should never
            // download the same image twice.
            def streamTail = stream(newVisitedURLs).noWaitingStream
            imageURL.fetch match {
              // We couldn't download the image.
              case None => None #:: streamTail
              // We did download the image.	
              case Some((image)) => Some(image) #:: streamTail
            }
          }
        }
    }
}

trait TwoPassStream2 extends RetryingImageStream {
  /**
   * A list of image URLs, sorted with the most desirable URL first.
   */
  def getImageURLs(cache: WebCache): Option[(Seq[ImageURL], WebCache)]

  /**
   *
   */
  def streamTail: (WebCache, Set[URL]) => Stream[Option[BufferedImage]]

  def cache: WebCache

  def visitedURLs: Set[URL]

  ///////////////////////////////////////////////////////////

  override def noWaitingImageStream = {
    getImageURLs(cache) match {
      case None => None #:: streamTail(cache, visitedURLs)
      case Some((imageURLs, newCache)) => {
        imageURLs.filter({
          case ImageURL(url) => !visitedURLs.contains(url)
        }).headOption match {
          case None =>
            None #:: streamTail(newCache, visitedURLs)
          case Some(imageURL @ ImageURL(url)) => {
            // We mark this url as seen regardless of whether we can 
            // download the image.
            val newVisitedURLs = visitedURLs + url

            // We don't cache the downloaded image, since we should never
            // download the same image twice.
            def streamTailFinal =
              streamTail(newCache, newVisitedURLs)
            imageURL.fetch match {
              case None => None #:: streamTailFinal
              case Some((image)) => Some(image) #:: streamTailFinal
            }
          }
        }
      }
    }
  }
}

object StreamBing {
  implicit class StreamBing2ImageStream(
    self: StreamBing) extends RetryingImageStream {
    override def waitBetweenAttemptsInSeconds = 5 * 60

    override def noWaitingImageStream = {
      // This URL has links to (all?) the Bing images of the day.
      // Thanks to the following for the URL:
      // http://dgoins.wordpress.com/2011/06/27/changing-the-gnome-3-desktop-with-images-from-bing/
      val bingURL = new URL(
        "http://themeserver.microsoft.com/default.aspx?p=Bing&c=Desktop&m=en-US")

      // Whenever we need a new image, we get the up-to-date list of images, and
      // return the newest one that we haven't already used.
      self.cache.get(DocumentURL(bingURL)) match {
        case None => None #:: self.noWaitingImageStream
        case Some((doc, newCache)) => {
          // Each URL is in an "item"
          val items = doc.getElementsByTag("item").toArray.toList.asInstanceOf[List[Element]]
          val datesAndURLs = (for (item <- items) yield {
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
          }) filter {
            // We want the most recent image we haven't used.
            case (date, url) => !self.visitedURLs.contains(url)
          }

          // If there's no new content, we return None and recurse using
          // the updated cache.
          datesAndURLs.sortBy(_._1).reverse.headOption match {
            case None =>
              None #:: self.copy(cache = newCache).noWaitingImageStream
            case Some((_, url)) => {
              // We mark this url as seen regardless of whether we can 
              // download the image.
              val newVisitedURLs = self.visitedURLs + url

              // We don't cache the downloaded image, since we should never
              // download the same image twice.
              def streamTail =
                StreamBing(newCache, newVisitedURLs).noWaitingImageStream
              ImageURL(url).fetch match {
                case None => None #:: streamTail
                case Some((image)) => Some(image) #:: streamTail
              }
            }
          }
        }
      }
    }
  }
}
    