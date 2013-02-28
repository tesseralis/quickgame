package models

import scala.util.{Try, Success, Failure}

import akka.actor.Actor

import play.api.Logger
import play.api.libs.json.{JsValue, Json, JsUndefined, JsString}
import play.api.libs.iteratee.{Iteratee, Concurrent}

// Classes to handle the room state
case class Join(username: String)
case class Quit(username: String)

/**
 * The GameRoom actor handles the logic for a single game room.
 * It adds and removes players, creates websockets, starts and stops the game, etc.
 */
trait GameRoom[State, Mov] extends Actor {
  /* 
   * Internal messages sent from the room's iteratee to itself.
   * Each represents a possible message sent from the client.
   */
  sealed trait ServerMessage
  case class ChangeRole(username: String, role: Int) extends ServerMessage
  case class RequestUpdate(username: String, data: Set[String]) extends ServerMessage
  case class Move(user: String, move: Mov) extends ServerMessage

  // The number of players needed to play the game
  def maxPlayers: Int

  // How to parse a JSON move
  def parseMove(input: JsValue): Option[Mov]

  def encodeState(input: State): JsValue

  def move(state: State, idx: Int, mv: Mov): Try[State]

  def initState: State

  def jsData(kind: String): JsValue = {
    val data: JsValue = kind match {
      case "members" => Json.arr(members.keys)
      case "players" => Json.arr((0 until maxPlayers).map { i =>
        playersByIndex.get(i).map(JsString).getOrElse(JsUndefined("no player"))
      })
      case "gamestate" => encodeState(state)
      case _ => JsUndefined("type not found")
    }
    Json.obj("kind" -> kind, "data" -> data)
  }

  def jsMessage(msg: String): JsValue = {
    Json.obj("kind" -> "message", "data" -> msg)
  }

  def sendAll(msg: JsValue) {
    for (channel <- members.values) {
      channel.push(msg)
    }
  }

  def serverMessage(id: String, kind: String, data: JsValue): Option[ServerMessage] = kind match {
    case "update" => data.asOpt[Set[String]] map { RequestUpdate(id, _) }
    case "changerole" => data.asOpt[Int] map { ChangeRole(id, _) }
    case "move" => parseMove(data) map { Move(id, _) }
    case _ => None
  }

  def iteratee(username: String) = Iteratee.foreach[JsValue] { event => 
    for (kind <- event.asOpt[String]; msg <- serverMessage(username, kind, event\"data")) {
      self ! msg
    }
  } mapDone { _ => self ! Quit(username) }

  // a mapping from the members of the room to their message channels.
  var members = Map[String, Concurrent.Channel[JsValue]]()
  // A map of players to their position in the game
  var players = Map[String, Int]()

  def playersByIndex: Map[Int, String] = players map { _.swap }

  var state = initState

  override def receive = {
    case Join(username) => {
      if (members contains username) {
        sender ! CannotConnect("This username is already in use.")
      } else {
        val (enumerator, channel) = Concurrent.broadcast[JsValue]
        members += (username -> channel)
        sendAll(jsData("members"))
        sender ! Connected(iteratee(username), enumerator)

        // Make this member a player if there are spots available
        if (players.size < maxPlayers) {
          players += (username -> (0 until maxPlayers).indexWhere(!playersByIndex.contains(_)))
          sendAll(jsData("players"))
        }
      }
    }
    case Quit(username) => {
      members -= username
      players -= username
      sendAll(jsData("members"))
      sendAll(jsData("players"))
    }
    case Move(username, mv) => {
      for (idx <- players.get(username)) {
        move(state, idx, mv) match {
          case Success(newState) => {
            state = newState
            sendAll(jsData("state"))
          }
          case Failure(e) =>
            members(username).push(jsMessage(s"You've made a bad move: $e"))
        }
      }
    }
    case ChangeRole(username, role) => {
      members.get(username) map { channel =>
        // Remove player if invalid number is given
        if (role <= 0 || role > maxPlayers) {
          players -= username
          sendAll(jsData("players"))
        } else if (playersByIndex.contains(role)) {
          members(username).push(jsMessage(s"That role is unavailable."))
        } else {
          players += (username -> role)
          sendAll(jsData("players"))
        }
      }
    }
    case RequestUpdate(username, data) => {
      for (kind <- data) {
        members(username).push(jsData(kind))
      }
    }
  }
}
