package test

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers

import play.api.test._
import play.api.test.Helpers._

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
class IntegrationSpec extends FlatSpec with ShouldMatchers {
  
  "Application" should "work from within a browser" in new WithBrowser {
      browser.goTo("/")
      browser.pageSource should include ("quickga.me")
  }
  
}
