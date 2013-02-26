package models

import scala.util._
import scala.concurrent.duration._
import scala.concurrent._

import akka.actor._

import akka.util.Timeout
import akka.pattern.ask

import play.api.libs.json._
import play.api.libs.concurrent._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits._

import play.api.Play.current

object GameManager {
  implicit val timeout = Timeout(1.second)
  lazy val default = Akka.system.actorOf(Props[GameManager])

  /**
   * Create a new game and return the generated id
   */
  def create(g: GameType): Future[Option[String]] = {
    (default ? CreateGame(g)) map {
      case Created(id) => Some(id)
    }
  }

  def generateId(isNew: String => Boolean): String = {
    val id = Random.alphanumeric.take(5).mkString
    if (isNew(id)) id else generateId(isNew)
  }

  /**
   * Join an existing game.
   */
  def join(g: GameType, id: String):
      scala.concurrent.Future[(Iteratee[JsValue,_], Enumerator[JsValue])] = {
    (default ? JoinGame(g, id)).map {
      case Connected(enumerator) =>
        // Create an Iteratee to consume the feed
        val iteratee = Iteratee.foreach[JsValue] { event =>
          //room ! Talk(username, (event \ "text").as[String])
        }.mapDone { _ =>
          //room ! Quit(username)
        }

        (iteratee, enumerator)

      case CannotConnect(error) =>
        // Connection error
        val iteratee = Done[JsValue, Unit]((), Input.EOF)
        val enumerator = Enumerator[JsValue](JsObject(Seq("error" -> JsString(error)))).andThen(Enumerator.enumInput(Input.EOF))

        (iteratee, enumerator)
    }
  }
}

/**
 * The singular object responsible for managing all our games.
 */
class GameManager extends Actor {
  // Mapping from game ids to gameroom references
  var games: Map[String, ActorRef] = Map.empty

  // TODO override the supervisor strategy

  def receive = {
    case CreateGame(g) => {
      val id = GameManager.generateId(!games.contains(_))
      // TODO: Different game types
      games = games.updated(id, Akka.system.actorOf(Props(new ChatRoom(id))))
      sender ! Created(id)
    }
  }
}

case class CreateGame(g: GameType)
case class JoinGame(g: GameType, id: String)

case class Created(id: String)
