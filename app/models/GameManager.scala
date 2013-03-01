package models

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.{ActorRef, TypedActor, TypedProps}
import akka.util.Timeout
import akka.pattern.ask

import play.api.libs.json.{JsValue, Json}
import play.api.libs.concurrent.Akka
// todo Learn more about execution contexts.
import play.api.libs.concurrent.Execution.Implicits._

import play.api.Play.current

import utils.{GameType, WebSocket}


object GameManager {

  /** Return the default game manager implemenation, an Akka typed actor. */
  def apply(): GameManager = TypedActor(Akka.system).typedActorOf(TypedProps[GameManagerImpl]())

  /** Create a new random ID string. */
  // todo Move to utils?
  @scala.annotation.tailrec
  def generateId(isNew: String => Boolean): String = {
    val id = scala.util.Random.alphanumeric.take(5).mkString
    if (isNew(id)) id else generateId(isNew)
  }

  /** Returns a websocket demonstrating that you've found an error. */
  // todo Move to utils?
  def errorWebSocket[A](error: A): WebSocket[A] = {
    import play.api.libs.iteratee._
    val iteratee = Done[A, Unit]((), Input.EOF)
    val enumerator = Enumerator[A](error).andThen(Enumerator.enumInput(Input.EOF))

    (iteratee, enumerator)
  }
}

trait GameManager {

  /** Create a new game of the specified type. */
  def create(g: GameType): Future[String]

  /** Check whether the given game exists. */
  def contains(g: GameType, id: String): Future[Boolean]

  /** Join the given game. */
  def join(g: GameType, id: String, username: String): Future[WebSocket[JsValue]]
}

// todo Should we make a RoomManager class to manage the rooms for each specific game?
// This will allow us to have matching akka and websocket paths
class GameManagerImpl extends GameManager {
  import GameManager._

  implicit val timeout = Timeout(10.seconds)

  val ctx = TypedActor.context

  // todo Can we use ctx.actorFor instead?
  // Right now I don't know how to differentiate between a legit room and a deadletter
  var games: Map[(GameType, String), ActorRef] = Map.empty

  override def create(g: GameType) = Future {
    val id = generateId(id => !games.contains((g, id)))
    games += ((g, id) -> ctx.actorOf(g.props))
    id
  }

  override def contains(g: GameType, id: String) = Future { games.contains((g, id)) }

  override def join(g: GameType, id: String, username: String) =
    games.get((g, id)) map { room =>
      (room ? Join(username)) map {
        // todo Get rid of the Connected/CannotConnect case classes...
        case Connected(iteratee, enumerator) => {
          (iteratee, enumerator)
        }
        case CannotConnect(error) => errorWebSocket[JsValue](Json.obj("error" -> error))
      }
    } getOrElse {
      Future(errorWebSocket[JsValue](Json.obj("error" -> s"Room $g/$id does not exist.")))
    }
}
