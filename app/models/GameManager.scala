package models

import scala.util._
import scala.concurrent.duration._
import scala.concurrent._

import akka.actor._

import akka.util.Timeout
import akka.pattern.ask

import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._

import play.api.Play.current

import GameType._

object GameManager {
  implicit val timeout = Timeout(1.second)
  lazy val default = Akka.system.actorOf(Props[GameManager])

  /**
   * Create a new game and return the generated id
   */
  def create(gameName: String): Future[Option[String]] = {
    (default ? CreateGame(gameName)) map {
      case Created(id) => Some(id)
      case NotCreated => None
    }
  }

  def generateId(isNew: String => Boolean): String = {
    val id = Random.alphanumeric.take(5).mkString
    if (isNew(id)) id else generateId(isNew)
  }

  /**
   * Join an existing game.
   */
  //def join(gameName: String, id: String): 
  //    scala.concurrent.Future[(Iteratee[JsValue,_], Enumerator[JsValue])] = {
  //  (default ? JoinGame(gameName, id)) map {
  //    case Connected
  //  }
  //}
}

/**
 * The singular object responsible for managing all our games.
 */
class GameManager extends Actor {
  // Mapping from game ids to gameroom references
  var games: Map[String, ActorRef] = Map.empty

  // TODO override the supervisor strategy

  def receive = {
    case CreateGame(gameName) => {
      Try(GameType withName gameName) match {
        case Success(_) =>
          val id = GameManager.generateId(!games.contains(_))
          // TODO Generalize for different game types
          games = games.updated(id, Akka.system.actorOf(Props(new ChatRoom(id))))
          sender ! Created(id)
        case Failure(e) => sender ! NotCreated
      }
    }
  }
}

case class CreateGame(gameName: String)
case class JoinGame(gameName: String, id: String)

case class Created(id: String)
case object NotCreated
