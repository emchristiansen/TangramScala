package tangram.stream

///////////////////////////////////////////////

sealed trait StreamError
object NoMoreImagesError extends StreamError
object FetchError extends StreamError