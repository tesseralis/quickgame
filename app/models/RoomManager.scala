package models

import scala.concurrent.duration._

import akka.actor.{Actor, Props}
import akka.util.Timeout
import akka.pattern.{ask, pipe}

import play.api.libs.concurrent.Execution.Implicits._

import common.GameType
import common.models.{Game, GameFormat}

object RoomManager {
  /** Tell the manager to create a room. */
  case object Create
  /** Ask the manager if a given room exists. */
  case class Contains(id: String)
  /** Ask the manager to join the given room, with a given username. */
  case class Join(id: String, name: Option[String])

  /** Create a new random ID string, in the style of collabedit's IDs. */
  @scala.annotation.tailrec
  def generateId(length: Int, isNew: String => Boolean = _ => true): String = {
    val id = scala.util.Random.alphanumeric.take(length).mkString.toLowerCase
    if (isNew(id)) id else generateId(length, isNew)
  }
}

/**
 * Manages the all the rooms of a given game type.
 */
class RoomManager(model: Game with GameFormat) extends Actor {
  import RoomManager._

  implicit val timeout = Timeout(10.seconds)

  override def receive = {
    case Create =>
      val id = generateId(5, id => context.child(id).isEmpty)
      context.actorOf(Props(new GameRoom(model)), id)
      sender ! id

    case Contains(id) =>
      sender ! (!context.child(id).isEmpty)

    case Join(id: String, name: Option[String]) =>
      context.child(id).foreach { room =>
        (room ? GameRoom.Join(name)) pipeTo sender
      }
  }
}
