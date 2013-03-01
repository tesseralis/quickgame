package controllers

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.mvc.{Controller, WebSocket, Action}
import play.api.libs.json.JsValue

import models.GameManager
import utils.GameType

object Application extends Controller {

  val gameManager = GameManager()
  
  def index = Action {
    Ok(views.html.index())
  }

  def gameIndex(g: GameType) = Action {
    Ok(views.html.gameIndex(g))
  }

  def newGame(g: GameType) = Action {
    Async {
      gameManager.create(g).map { id =>
        Redirect(routes.Application.game(g, id, None))
      }
    }
  }

  // TODO Move the username out of the request parameter, cause that's just weird
  def game(g: GameType, id: String, username: Option[String]) = Action { implicit request =>
    Async {
      gameManager.contains(g, id) map { gameFound =>
        if (gameFound) {
          val username1 = username getOrElse scala.util.Random.alphanumeric.take(10).mkString
          Ok(g.view(id, username1, request))
        } else {
          NotFound(s"Could not find $g game #$id")
        }
      }
    }
  }

  def stream(g: GameType, id: String, username: String) = WebSocket.async[JsValue] { request =>
    gameManager.join(g, id, username)
  }

}
