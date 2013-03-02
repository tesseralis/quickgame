package models

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor._
import akka.util.Timeout
import akka.pattern.{ask, pipe}

import play.api.libs.json.{JsValue, Json}
import play.api.libs.concurrent.Akka
// todo Learn more about execution contexts.
import play.api.libs.concurrent.Execution.Implicits._

import play.api.Play.current

import utils.{GameType, WebSocket, generateId}


object GameManager {

  /** Return the default game manager implemenation, an Akka typed actor. */
  def apply(): GameManager = TypedActor(Akka.system).typedActorOf(TypedProps[GameManagerImpl]())


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

  /**
   * Join the given game.
   * @param name The name of the user joining. A default name will be generated if not given.
   */
  def join(g: GameType, id: String, name: Option[String]): Future[WebSocket[JsValue]]
}

class GameManagerImpl extends GameManager with TypedActor.PreStart {
  import GameManager._

  implicit val timeout = Timeout(10.seconds)

  val ctx = TypedActor.context

  var managers: Map[GameType, ActorRef] = Map.empty

  override def preStart() {
    // Create managers for every type of game available
    for (g <- controllers.Games.values) {
      managers += (g -> ctx.actorOf(Props(new RoomManager(g)), name=g.toString))
    }
  }

  override def create(g: GameType) = {
    (managers(g) ? RoomManager.Create).mapTo[String]
  }

  override def contains(g: GameType, id: String) = {
    (managers(g) ? RoomManager.Contains(id)).mapTo[Boolean]
  }

  override def join(g: GameType, id: String, name: Option[String]) = {
    (managers(g) ? RoomManager.Join(id, name)).mapTo[WebSocket[JsValue]]
  }
}

object RoomManager {
  case object Create
  case class Contains(id: String)
  case class Join(id: String, name: Option[String])
}

class RoomManager(g: GameType) extends Actor {
  implicit val timeout = Timeout(10.seconds)
  import RoomManager._
  override def receive = {
    case Create =>
      val id = generateId(5, id => context.child(id).isEmpty)
      context.actorOf(g.props, id)
      sender ! id

    case Contains(id) =>
      sender ! (!context.child(id).isEmpty)
    case Join(id: String, name: Option[String]) =>
      context.child(id).foreach { room =>
        (room ? GameRoom.Join(name)) pipeTo sender
      }
  }
}
