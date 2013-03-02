package models

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor._
import akka.util.Timeout
import akka.pattern.ask

import play.api.libs.json.JsValue
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._

import play.api.Play.current

import utils.{GameType, WebSocket}


object GameManager {

  /** Return the default game manager implemenation, an Akka typed actor. */
  def apply(types: Iterable[GameType]): GameManager =
    TypedActor(Akka.system).typedActorOf(TypedProps(classOf[GameManager],
      new GameManagerImpl(types)), "g")
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

class GameManagerImpl(games: Iterable[GameType]) extends GameManager {
  import GameManager._

  implicit val timeout = Timeout(10.seconds)

  val ctx = TypedActor.context

  val managers: Map[GameType, ActorRef] = games.map { g =>
    (g -> ctx.actorOf(Props(new RoomManager(g)), name=g.toString))
  }.toMap

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

