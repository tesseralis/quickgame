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
  type WebSocket[A] = (Iteratee[A, _], Enumerator[A])
  implicit val timeout = Timeout(1.second)
  private[this] lazy val default = Akka.system.actorOf(Props[GameManager])

  /**
   * Create a new random ID string.
   */
  def generateId(isNew: String => Boolean): String = {
    val id = Random.alphanumeric.take(5).mkString
    if (isNew(id)) id else generateId(isNew)
  }

  /**
   * Create a new game and return the generated id.
   */
  def create(g: GameType): Future[String] = {
    // bit of a hack to enforce string
    (default ? CreateGame(g)) map { case Symbol(id) => id }
  }

  /**
   * Check whether a game of the specified type and id exists.
   */
  def contains(g: GameType, id: String): Future[Boolean] = {
    default ? ContainsGame(g, id) map {
      case true => true
      case _ => false
    }
  }

  /**
   * Join an existing game.
   */
  def join(g: GameType, id: String, username: String): Future[WebSocket[JsValue]] = {

    // TODO Delegate this to the child classes
    (default ? JoinGame(g, id, username)).map {
      case Connected(enumerator) =>
        // Create an Iteratee to consume the feed
        val iteratee = Iteratee.foreach[JsValue] { event =>
          default ! PassMessage(g, id, Talk(username, (event \ "text").as[String]))
        }.mapDone { _ =>
          default ! PassMessage(g, id, Quit(username))
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
  implicit val timeout = Timeout(1.second)
  // Mapping from game ids to gameroom references
  var games: Map[(GameType, String), ActorRef] = Map.empty

  // TODO override the supervisor strategy

  def receive = {
    case CreateGame(g) => {
      val id = GameManager.generateId(id => !games.contains((g, id)))
      // TODO: Different game types
      games += ((g, id) -> Akka.system.actorOf(Props(new ChatRoom(id))))
      sender ! Symbol(id)
    }
    case ContainsGame(g, id) => sender ! games.contains((g, id))
    case JoinGame(g, id, username) => {
      games.get((g, id)) map { room =>
        (room ? Join(username)) map { result => sender ! result }
      } getOrElse {
        sender ! CannotConnect(s"Cannot find the $g game #$id.")
      }
    }
    case PassMessage(g, id, msg) => {
      games.get((g, id)) map { room =>
        room ! msg
      } getOrElse {
        sender ! RoomNotFound(s"Cannot find $g/$id")
      }
    }
  }
}

case class CreateGame(g: GameType)
case class JoinGame(g: GameType, id: String, username: String)
case class ContainsGame(g: GameType, id: String)
case class PassMessage(g: GameType, id: String, msg: Any)

case class RoomNotFound(msg: String)
