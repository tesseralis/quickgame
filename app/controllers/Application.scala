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
import utils._

object Application extends Controller {

  val gameManager: GameManager = TypedActor(Akka.system).typedActorOf(
    TypedProps[GameManagerImpl]()
  )
  
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
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

  def game(g: GameType, id: String, username: Option[String]) = Action { implicit request =>
    Async {
      gameManager.contains(g, id) map { gameFound =>
        if (gameFound) {
          val username1 = username getOrElse Random.alphanumeric.take(10).mkString
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
