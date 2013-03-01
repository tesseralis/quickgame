package models

import scala.util.{Try, Success, Failure}

import akka.actor.Actor

import play.api.libs.json.{JsValue, Json, JsUndefined, JsString}
import play.api.libs.iteratee.{Iteratee, Concurrent}

import utils.generateId

// Classes to handle the room state
case class Join(name: Option[String])
case class Quit(uid: String)

object RoomState extends Enumeration {
  type RoomState = Value
  val Playing, Paused, Lobby = Value
}


// todo Switch to an FSM model for this.
/**
 * The GameRoom actor handles the logic for a single game room.
 * It adds and removes players, creates websockets, starts and stops the game, etc.
 */
trait GameRoom[State, Mov] extends Actor {
  import RoomState._
  /* 
   * Internal messages sent from the room's iteratee to itself.
   * Each represents a possible message sent from the client.
   */
  sealed trait ServerMessage
  case class ChangeRole(uid: String, role: Int) extends ServerMessage
  case class ChangeName(uid: String, name: String) extends ServerMessage
  case class RequestUpdate(uid: String, data: Set[String]) extends ServerMessage
  case class Move(uid: String, move: Mov) extends ServerMessage
  case class Message(uid: String, text: String) extends ServerMessage

  // The number of players needed to play the game
  def maxPlayers: Int

  // How to parse a JSON move
  def parseMove(input: JsValue): Option[Mov]

  def encodeState(input: State): JsValue

  def move(state: State, idx: Int, mv: Mov): Try[State]

  def initState: State

  def jsData(kind: String): JsValue = {
    val data: JsValue = kind match {
      case "members" => Json.arr(members.keys.map(usernames))
      case "players" => Json.toJson((0 until maxPlayers).map { i =>
        playersByIndex.get(i).map(usernames).getOrElse("")
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

  def serverMessage(uid: String, kind: String, data: JsValue): Option[ServerMessage] = kind match {
    case "update" => data.asOpt[Array[String]] map { x => RequestUpdate(uid, x.toSet) }
    case "changerole" => data.asOpt[Int] map { ChangeRole(uid, _) }
    case "changename" => data.asOpt[String] map { ChangeName(uid, _) }
    case "move" => parseMove(data) map { Move(uid, _) }
    case "message" => data.asOpt[String] map { Message(uid, _) }
    case _ => None
  }

  def iteratee(uid: String) = Iteratee.foreach[JsValue] { event => 
    for (kind <- (event\"kind").asOpt[String]; msg <- serverMessage(uid, kind, event\"data")) {
      self ! msg
    }
  } mapDone { _ => self ! Quit(uid) }

  // todo switch to actor m odel for players
  // a mapping from the members of the room to their message channels.
  var members = Map[String, Concurrent.Channel[JsValue]]()
  // A map of players to their position in the game
  var players = Map[String, Int]()
  // Store the names of the members
  var usernames = Map[String, String]()

  def playersByIndex: Map[Int, String] = players map { _.swap }

  var state: State = initState

  var roomState: RoomState = Lobby

  override def receive = {
    case Join(nameOpt) => {
      val uid = generateId(10, (!members.contains(_)))
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      members += (uid -> channel)
      usernames += (uid -> nameOpt.getOrElse(generateId(10)))
      sendAll(jsData("members"))
      sender ! (iteratee(uid), enumerator)

      // Make this member a player if there are spots available
      if (players.size < maxPlayers) {
        players += (uid -> (0 until maxPlayers).indexWhere(!playersByIndex.contains(_)))
        sendAll(jsData("players"))
      }

      // If we have the required number of players, start or resume the game
      if (players.size == maxPlayers && roomState != Playing) {
        if (roomState == Lobby) {
          state = initState
        }
        roomState = Playing
      }
    }
    case Quit(uid) => {
      members -= uid
      players -= uid
      sendAll(jsData("members"))
      sendAll(jsData("players"))
      if (roomState == Playing && players.size <= maxPlayers) {
        roomState = Paused
      }
    }
    case Move(uid, mv) => {
      for (idx <- players.get(uid)) {
        move(state, idx, mv) match {
          case Success(newState) => {
            state = newState
            sendAll(jsData("gamestate"))
          }
          case Failure(e) =>
            members(uid).push(jsMessage(s"You've made a bad move: $e"))
        }
      }
    }
    case Message(uid, message) => {
      sendAll(jsMessage(s"${usernames(uid)}: $message"))
    }
    case ChangeRole(uid, role) => {
      members.get(uid) map { channel =>
        if (roomState == Playing) {
          members(uid).push(jsMessage(s"Cannot change roles in the middle of a game."))
        } else {
          // Remove player if invalid number is given
          if (role <= 0 || role > maxPlayers) {
            players -= uid
            sendAll(jsData("players"))
          } else if (playersByIndex.contains(role)) {
            members(uid).push(jsMessage(s"That role is unavailable."))
          } else {
            players += (uid -> role)
            sendAll(jsData("players"))
          }
        }
      }
    }
    case ChangeName(uid, name) => {
      members.get(uid) map { channel =>
        usernames += (uid -> name)
        sendAll(jsData("players"))
        sendAll(jsData("members"))
      }
    }

    case RequestUpdate(uid, data) => {
      for (kind <- data) {
        members(uid).push(jsData(kind))
      }
    }
  }
}
