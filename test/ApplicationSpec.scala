package test

import org.scalatest._
import org.scalatest.OptionValues._
import org.scalatest.matchers.ShouldMatchers

import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class ApplicationSpec extends FlatSpec with ShouldMatchers {
  
  "Application" should "render the index page" in new WithApplication {
    val home = route(FakeRequest(GET, "/")).get
    status(home) should be (OK)
    contentType(home).value should be ("text/html")
    contentAsString(home) should include ("quickga.me")
  }
}
