package models

import akka.actor.Actor

import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.iteratee.{Iteratee, Concurrent}

trait GameRoom[MoveType] extends Actor {
  // The current members of this room
  var members: Set[String]

  // The function to call on a join
  def join(username: String): Unit

  // How to parse a JSON move
  def parseMove(input: JsValue): MoveType

  // The function to call on quit
  def quit(username: String): Unit

  private[this] def iteratee(username: String) = Iteratee.foreach[JsValue] { move =>
    self ! GameMove(username, parseMove(move))
  } mapDone { _ => self ! Quit(username) }

  val (enumerator, channel) = Concurrent.broadcast[JsValue]

  override def receive = {
    case Join(username) => {
      if (members contains username) {
        sender ! CannotConnect("This username is already in use.")
      } else {
        members += username
        join(username)
        sender ! Connected(iteratee(username), enumerator)
      }
    }
    case Quit(username) => {
      members -= username
      quit(username)
    }
  }
}
