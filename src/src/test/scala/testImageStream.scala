package tangram.test

import tangram._
import tangram.stream._
import tangram.streams._
import tangram.style._
//import tangram.styles._

import CachedImageStream._

import org.scalatest.FunSuite

///////////////////////////////////////////////////////////

class TestImageStream extends FunSuite {
  test("ensure implicit conversions are available") {
    val imageStream: ImageStream = StreamBing()
  }
}