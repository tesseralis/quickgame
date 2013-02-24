package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent._

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def gameIndex(gameName: String) = Action {
    if (gameName == "tictactoe") {
      Ok("Welcome to our tic-tac-toe page!")
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

  def stream(gameName: String, id: String) = TODO
//  def stream(gameName: String, id: String) = WebSocket.async[JsValue] { request =>
//    Promise.pure(0)
//  }
}
