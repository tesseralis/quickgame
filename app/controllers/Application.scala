package controllers

import scala.util.Random

import akka.actor._

import play.api._
import play.api.Play.current
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.concurrent._

import models._

object Application extends Controller {

  // TODO perhaps a GameManager actor that keeps track of the current games
  var games: Map[String, Map[String, ActorRef]] = Map("tictactoe" -> Map.empty, "chat" -> Map.empty)
  
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def newGame(gameName: String) = Action {
    if (games contains gameName) {
      var id = "";
      do {
        id = Random.alphanumeric.take(5).mkString
      } while (games(gameName).contains(id))

      val actor = Akka.system.actorOf(Props(new ChatRoom(id)))
      games = games.updated(gameName, games(gameName).updated(id, actor))
      Redirect(routes.Application.game(gameName, id))
    } else {
      NotFound("Game not implemented")
    }
  }

  def gameIndex(gameName: String) = Action {
    if (games contains gameName) {
      Ok(views.html.gameIndex(gameName))
    } else {
      NotFound("Sorry, this game hasn't been implemented yet.")
    }
  }

  def game(gameName: String, id: String) = Action {
    if (games contains gameName) {
      games(gameName).get(id).map { _ =>
        Ok(views.html.chat(id))
      } getOrElse {
        NotFound("Game not found")
      }
    } else {
      NotFound("Page not found")
    }
  }

  def stream(gameName: String, id: String) = WebSocket.async[JsValue] { request =>
    if (games.contains(gameName) && games(gameName).contains(id)) {
      ChatRoom.join(games(gameName)(id), Random.alphanumeric.take(10).mkString)
    } else {
      throw new Error("TODO this should be replaced by something nicer.")
    }
    
  }
}
