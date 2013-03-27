package controllers

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.mvc.{Controller, WebSocket, Action}
import play.api.libs.json.JsValue

import models.GameManager
import common.{GameType, GameAdapter}

object Application extends Controller {
  /*
   * List of the avalailable games.
   * Add new games to this list in order to make them available to QuickGame.
   */
  private[this] val _games: Seq[GameAdapter] = Seq(
    games.tictactoe.Adapter,
    games.connectfour.Adapter
  )

  /*
   * Convenience mappings from the game type to the model or view.
   */
  val gameViews = _games.map(ctrl => ctrl.gameType -> ctrl.view).toMap
  val gameModels = _games.map(ctrl => ctrl.gameType -> ctrl.model).toMap
  val gameTypes = _games.map(_.gameType)

  /** The manager actor. */
  val gameManager = GameManager(gameModels)
  
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
          Ok(views.html.game(g, id)(gameViews(g)))
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
