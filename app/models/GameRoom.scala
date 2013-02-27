package models

import scala.util.{Try, Success, Failure}

import akka.actor.Actor

import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.libs.iteratee.{Iteratee, Concurrent}

case class Join(username: String)
case class Quit(username: String)
case class Talk(username: String, text: String)
case class UpdateRole(username: String, role: Int)
case class RequestState(username: String)

trait GameRoom[State, Mov] extends Actor {
  case class Move(user: String, move: Mov)

  // The number of players needed to play the game
  def maxPlayers: Int

  // How to parse a JSON move
  def parseMove(input: JsValue): Mov

  def encodeState(input: State): JsValue

  def move(state: State, idx: Int, mv: Mov): Try[State]

  def initState: State

  // TODO Bring together JSON data format.
  private[this] def notifyAll(kind: String, user: String) {
    val msg = Json.obj(
      "kind" -> kind,
      "user" -> user,
      "state" -> encodeState(state),
      "members" -> Json.arr(members.keys)
      // TODO List players
    )
    // TODO: Can we make this asynchronous?
    for (channel <- members.values) {
      channel.push(msg)
    }
  }

  def notify(user: String, kind: String, data: String) {
    val msg = Json.obj(
      "kind" -> kind,
      "data" -> data
    )
    members(user).push(msg)
  }

  def iteratee(username: String) = Iteratee.foreach[JsValue] { evt => 
    (evt\"kind").as[String] match {
      // Move the player
      case "move" => self ! Move(username, parseMove(evt))
      // Update a player role
      case "update" => self ! UpdateRole(username, (evt\"role").as[Int])
      // Request the game state
      case "request" => self ! RequestState(username)
    }
  } mapDone { _ => self ! Quit(username) }

  // a mapping from the members of the room to their message channels.
  var members = Map[String, Concurrent.Channel[JsValue]]()
  // A map of players to their position in the game
  var players = Map[String, Int]()

  var state = initState

  override def receive = {
    case Join(username) => {
      if (members contains username) {
        sender ! CannotConnect("This username is already in use.")
      } else {
        if (players.size < maxPlayers) {
          players += (username -> (0 until maxPlayers).indexWhere(!players.values.toSet.contains(_)))
        }
        val (enumerator, channel) = Concurrent.broadcast[JsValue]
        members += (username -> channel)
        sender ! Connected(iteratee(username), enumerator)
        notifyAll("join", username)
      }
    }
    case Quit(username) => {
      members -= username
      players -= username
      notifyAll("quit", username)
    }
    case Move(username, mv) => {
      for (idx <- players.get(username)) {
        move(state, idx, mv) match {
          case Success(newState) => {
            state = newState
            notifyAll("move", username)
          }
          case Failure(e) =>
            notify(username, "error", s"You've made a bad move: $e")
        }
      }
    }
    case UpdateRole(username, role) => {
      members.get(username) map { channel =>
        // Remove player if invalid number is given
        if (role <= 0 || role > maxPlayers) {
          players -= username
          notifyAll("update", username)
        } else if (players.values.toSet.contains(role)) {
          notify(username, "error", "That role is already taken.")
        } else {
          players += (username -> role)
          notifyAll("update", username)
        }
      }
    }
    case RequestState(username) => {
      // TODO send an individual message
      notifyAll("update", username)
    }
  }
}
