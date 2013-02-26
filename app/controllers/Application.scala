package controllers

import scala.util._
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor._

import play.api._
import play.api.Play.current
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.concurrent._
import play.api.mvc.BodyParsers._

import models._

object Application extends Controller {

  // TODO perhaps a GameManager actor that keeps track of the current games
  //var games: Map[String, Map[String, ActorRef]] = Map("tictactoe" -> Map.empty, "chat" -> Map.empty)
  var games: Map[GameType, Map[String, ActorRef]] = Map(
    Chat -> Map.empty,
    Tictactoe -> Map.empty
  )
  
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def newGame(g: GameType) = Action {
    var id = "";
    do {
      id = Random.alphanumeric.take(5).mkString
    } while (games(g).contains(id))

    val actor = Akka.system.actorOf(Props(new ChatRoom(id)))
    games = games.updated(g, games(g).updated(id, actor))
    // TODO Add username parameter for creating the chat room
    Redirect(routes.Application.game(g, id, None))
  }


  def gameIndex(g: GameType) = Action {
    Ok(views.html.gameIndex(g))
  }

  def game(g: GameType, id: String, username: Option[String]) = Action { implicit request =>
    // use a random username if none is given.
    games(g).get(id).map { _ => 
      val username1 = username getOrElse Random.alphanumeric.take(10).mkString
      g match {
        case Tictactoe => Ok(views.html.chat(id, username1))
        case Chat => Ok(views.html.chat(id, username1))
      }
    } getOrElse {
      NotFound("Game not found")
    }
  }

  def stream(g: GameType, id: String, username: String) = WebSocket.async[JsValue] { request =>
    if (games(g).contains(id)) {
      ChatRoom.join(games(g)(id), username)
    } else {
      throw new Error("TODO this should be replaced by something nicer.")
    }
  }

  def newGame2(g: GameType) = Action {
    Async {
      GameManager.create(g).map { id =>
        Redirect(routes.Application.game(g, id, None))
      }
    }
  }

  def game2(g: GameType, id: String, username: Option[String]) = Action { implicit request =>
    Async {
      GameManager.contains(g, id) map { gameFound =>
        if (gameFound) {
          val username1 = username getOrElse Random.alphanumeric.take(10).mkString
          g match {
            case Tictactoe => Ok(views.html.chat(id, username1))
            case Chat => Ok(views.html.chat(id, username1))
          }
        } else {
          NotFound(s"Could not find $g game #$id")
        }
      }
    }
  }

  def stream2(g: GameType, id: String, username: String) = WebSocket.async[JsValue] { request =>
    GameManager.join(g, id, username)
  }

}
