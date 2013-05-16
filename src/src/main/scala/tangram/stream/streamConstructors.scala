package tangram.stream

import scala.collection.immutable.Stream.consWrapper

///////////////////////////////////////////////////////////

object PessimisticProcessor {  
  implicit def PessimisticProcessor2ProcessPossibleImageURLs(
    self: PessimisticProcessor.type): ProcessPossibleImageURLs =
    possibleImageURLs => {
      possibleImageURLs map {
        case Right(ImageURL) => ImageURL
        case Left => throw new Error
      }
    }
}

///**
// * An image stream that might not always provide an image.
// */
//trait FailingImageStream extends ImageStream {
//  def failingImageStream: Stream[Option[Image]]
//
//  /////////////////////////////////////////////////////////
//
//  override def imageStream = failingImageStream.flatten
//}
//
///**
// * An image stream that sleeps then retries when it cannot get the next
// * image.
// */
//trait RetryingImageStream extends FailingImageStream {
//  def waitBetweenAttemptsInSeconds: Int
//
//  /**
//   * An image stream that does not sleep between retries.
//   */
//  // TODO: This can probably be turned into a mixin.
//  def noWaitingImageStream: Stream[Option[BufferedImage]]
//
//  /////////////////////////////////////////////////////////  
//
//  override def failingImageStream = noWaitingImageStream map {
//    // If the image was successfully retrieved, we insert no
//    // delay.
//    case Some(image) => Some(image)
//
//    // If the image was not retrieved, we sleep before returning None.
//    case None => {
//      println(s"No new images in stream, sleeping ${waitBetweenAttemptsInSeconds}s")
//      Thread.sleep(waitBetweenAttemptsInSeconds * 1000)
//      None
//    }
//  }
//}

