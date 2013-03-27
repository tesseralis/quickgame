package models

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor._
import akka.util.Timeout
import akka.pattern.ask

import play.api.libs.json.JsValue
import play.api.libs.iteratee.{Iteratee, Enumerator}
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._

import play.api.Play.current

import common.GameType
import common.models.{Game, GameFormat}


object GameManager {
  /**
   * Return the default game manager implemenation, an Akka typed actor.
   * @param types The game types available for this manager.
   */
  def apply(types: Map[GameType, Game with GameFormat]): GameManager =
    TypedActor(Akka.system).typedActorOf(TypedProps(classOf[GameManager],
      new GameManagerImpl(types)), "g")
}

trait GameManager {
  type WebSocket[A] = (Iteratee[A, _], Enumerator[A])

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

class GameManagerImpl(games: Map[GameType, Game with GameFormat]) extends GameManager {

  implicit val timeout = Timeout(10.seconds)

  val ctx = TypedActor.context

  val managers: Map[GameType, ActorRef] = games.map { case (g, model) =>
    (g -> ctx.actorOf(Props(new RoomManager(model)), name=g.toString))
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

