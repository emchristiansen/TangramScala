package photostream

import java.awt.image.BufferedImage
import java.net.URL
import java.util.Date
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.imageio.ImageIO
import java.net.SocketTimeoutException

///////////////////////////////////////////////////////////

// Caches pages locally to avoid lots of web activity. Pages eventually expire.
// TODO: This should be implicit, not global.
object DocumentCacher {
  val shelfLifeInSeconds = 60 * 60

  case class CachedDocument(document: Any, date: Date) {
    def fresh: Boolean = {
      val now = new Date()
      (now.getTime - date.getTime) / 1000 < shelfLifeInSeconds
    }
  }

  private val cache = new collection.mutable.HashMap[URL, Option[CachedDocument]] {
    override def default(key: URL) = None
  }

  // TODO: Make this mess type-safe.
  def get(url: URL, getter: URL => Any): Any = {
    cache(url) match {
      case Some(cached) if cached.fresh => cached.document
      case _ => {
        println("Getting %s".format(url))

        // Keeps trying to download until success.
        // TODO: Replace with better logic.
        val document = try {
          getter(url)
        } catch {
          case _: SocketTimeoutException => get(url, getter)
        }

        val cached = CachedDocument(
          document,
          new Date())
        cache(url) = Some(cached)
        get(url, getter)
      }
    }
  }

  def getDocument(url: URL): Document =
    get(url, url => Jsoup.connect(url.toString).get()).asInstanceOf[Document]

  def getImage(url: URL): BufferedImage =
    get(url, url => ImageIO.read(url)).asInstanceOf[BufferedImage]
}