package tangram

import nebula.imageProcessing._

///////////////////////////////////////////////

package object stream {  
  /**
   * Either an ImageURL or an Error.
   */
  type PossibleImageURL = Either[StreamError, ImageURL]
  
  type PossibleImageURLs = Stream[PossibleImageURL]
  
  type ImageURLs = Stream[ImageURL]
  
  type ProcessPossibleImageURLs = PossibleImageURLs => ImageURLs
  
  ////////////////////////////////////
  
  type PossibleImage = Either[StreamError, Image]
  
  type PossibleImages = Stream[PossibleImage]
  
  type PossiblyFetchImages = ImageURLs => PossibleImages
  
  ////////////////////////////////////
  
  type Images = Stream[Image]
  
  type ProcessPossibleImages = PossibleImages => Images
}