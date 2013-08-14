package tangram.stream

import tangram.stream.WebCache

import tangram.stream.ImageURL

///////////////////////////////////////////////////////////

/**
 * Returns a list of URLs that point to images on the web.
 * Takes a web cache for speeding list retrieval, and returns the updated
 * cache. 
 */
trait ImageFinder {
  def getImageURLs: WebCache => (List[ImageURL], WebCache)
}