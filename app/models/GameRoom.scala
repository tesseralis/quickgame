package models

import akka.actor.Actor

import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.iteratee.{Iteratee, Concurrent}

trait GameRoom[State, Mov] extends Actor {
  case class Move(user: String, move: Mov)
  // The current members of this room
  var members = Set[String]()

  // How to parse a JSON move
  def parseMove(input: JsValue): Mov

  // The function to call on a join
  def join(username: String)

  // The function to call on quit
  def quit(username: String)

  // the function to call on move
  def move(username: String, move: Mov)

  def sendState(username: String, state: State)


  private[this] def iteratee(username: String) = Iteratee.foreach[JsValue] { move =>
    self ! Move(username, parseMove(move))
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
    case Move(username, mv) => {
      move(username, mv)
    }
  }
}
