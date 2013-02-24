package controllers

import scala.util.Random

import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.concurrent._

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def newGame(gameName: String) = Action {
    val id: String = Random.alphanumeric.take(5).mkString
    Redirect(routes.Application.game(gameName, id))
  }

  def gameIndex(gameName: String) = Action {
    if (gameName == "tictactoe") {
      Ok(views.html.gameIndex(gameName))
    } else {
      NotFound("Sorry, this game hasn't been implemented yet.")
    }
  }

  def game(gameName: String, id: String) = Action {
    if (gameName == "tictactoe") {
      Ok(views.html.tictactoe(id))
    } else {
      NotFound("Page not found")
    }
  }

  def stream(gameName: String, id: String) = WebSocket.async[String] { request =>

    val in = Iteratee.foreach[String](println).mapDone { _ =>
      println("Disconnected")
    }

    val out = Enumerator("Hello!")

    Promise.pure((in, out))
  }
}
