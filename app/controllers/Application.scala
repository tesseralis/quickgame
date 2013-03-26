package controllers

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.mvc.{Controller, WebSocket, Action}
import play.api.libs.json.JsValue

import models.GameManager
import common.GameType

object Application extends Controller {

  val gamesAvailable: Set[GameType] = Set(games.tictactoe.TicTacToe, games.connectfour.ConnectFour)

  val gameManager = GameManager(gamesAvailable)
  
  def index = Action {
    Ok(views.html.index())
  }

  def gameIndex(g: GameType) = Action {
    Ok(views.html.gameIndex(g))
  }

  def newGame(g: GameType) = Action {
    Async {
      gameManager.create(g).map { id =>
        Redirect(routes.Application.game(g, id))
      }
    }
  }

  def game(g: GameType, id: String) = Action { implicit request =>
    Async {
      gameManager.contains(g, id) map { gameFound =>
        if (gameFound) {
          Ok(views.html.game(g, id)(g.view))
        } else {
          NotFound(s"Could not find $g game #$id")
        }
      }
    }
  }

  def socket(g: GameType, id: String, username: Option[String]) = WebSocket.async[JsValue] { request =>
    gameManager.join(g, id, username)
  }

  def setCookie(name: String) = Action {
    Ok("Set the username").withSession(
      "name" -> name
    )
  }

}
