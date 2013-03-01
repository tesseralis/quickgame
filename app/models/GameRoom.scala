package models

import scala.util.{Try, Success, Failure}
import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import play.api.libs.json._
import play.api.libs.iteratee.{Iteratee, Concurrent}
import play.api.libs.concurrent.Execution.Implicits._

import utils.generateId

// Classes to handle the room state
case class Join(name: Option[String])
case class Quit(uid: String)

object RoomState extends Enumeration {
  type RoomState = Value
  val Playing, Paused, Lobby = Value
}

/**
 * Message representation of a message from the client to the server.
 */
sealed trait ServerMessage
case class ChangeRole(role: Int) extends ServerMessage
case class ChangeName(name: String) extends ServerMessage
case object Restart extends ServerMessage
case class RequestUpdate(data: Set[String]) extends ServerMessage
case class Chat(text: String) extends ServerMessage


/**
 * Message representation of a message sent from the server to the client.
 */
sealed trait ClientMessage[A] {
  def data: A
  def dataToJson: JsValue

  def kindToJson: JsValue = JsString(toString.toLowerCase)
  
  def toJson: JsValue = Json.obj(
    "kind" -> kindToJson,
    "data" -> dataToJson
  )
}

case class Members(data: Seq[String]) extends ClientMessage[Seq[String]] {
  override def dataToJson = JsArray(data.map(JsString))
}
case class Players(data: Seq[String]) extends ClientMessage[Seq[String]] {
  override def dataToJson = JsArray(data.map(JsString))
}
case class Message(data: String) extends ClientMessage[String] {
  override def dataToJson = JsString(data)
}

class Client(messageFromJson: JsValue => Option[ServerMessage]) extends Actor {
  val (enumerator, channel) = Concurrent.broadcast[JsValue]
  val iteratee = Iteratee.foreach[JsValue] { json =>
    for (msg <- messageFromJson(json)) {
      context.parent ! msg
    }
  } mapDone { _ => context.stop(self) }

  override def receive = {
    // Pass the websocket up
    case Join(name) => sender ! (iteratee, enumerator)

    case msg: ClientMessage[_] =>
      channel.push(msg.toJson)
  }
}

trait Room[State, Mov] extends Actor {
  import RoomState._

  implicit val timeout = Timeout(10.seconds)

  /*
   * The following defines the public interface for this class.
   * Override these methods to implement room behavior.
   */

  /** The number of players needed to play the game. */
  def maxPlayers: Int

  /** How to convert a move from JSON input. */
  def moveFromJson(input: JsValue): Option[Mov]

  /** How to transform a state into JSON. */
  def stateToJson(input: State): JsValue

  /** Move from one action to another. */
  def move(state: State, idx: Int, mv: Mov): Try[State]

  /** Initial state of the game. */
  def initState: State

  /** Marker on the end of the game. */
  def gameEnd(state: State): Boolean


  def dataFromJson(kind: String, data: JsValue): Option[ServerMessage] = {
    None // todo implement...
  }

  /* Private state variables (to be replaced with an FSM */
  /** A list of names of members. */
  private[this] var members = Map[ActorRef, String]()
  /** A list of current players. */
  private[this] var players = Map[ActorRef, Int]()
  /** The state of the game. */
  private[this] var gameState: State = initState
  /** The state of the room. */
  private[this] var roomState: RoomState = Lobby

  /** Utility functions that transform our stored data. */
  def playersByIndex: Map[Int, ActorRef] = players map { _.swap }
  def memberNames = members.values.toSeq
  def playerNames = (0 until maxPlayers).map {i => 
    playersByIndex.get(i).map(members).getOrElse("")
  }

  /* Additional messages specific to states. */
  case class Move(move: Mov) extends ServerMessage
  case class GameState(data: State) extends ClientMessage[State] {
    override def dataToJson = stateToJson(gameState)
  }

  /** Send a message to all members */
  def notifyAll[A](msg: ClientMessage[A]) {
    for (child <- context.children) {
      child ! msg
    }
  }

  override def receive = {
    case msg @ Join(name) => {
      // Constructor?
      val client = context.actorOf(Props[Client])
      context.watch(client)

      // Send the websocket to the sender
      (client ? msg) pipeTo sender

      // Assign the name to the member
      members += (client -> name.getOrElse(generateId(10)))
      notifyAll(Members(memberNames))

      // Make a player if spots are available
      if (players.size < maxPlayers) {
        players += (client -> (0 until maxPlayers).indexWhere(!playersByIndex.contains(_)))
        notifyAll(Players(playerNames))
      }

      // If we have the required number of players, start or resume the game
      if (players.size == maxPlayers && roomState != Playing) {
        if (roomState == Lobby) {
          gameState = initState
          notifyAll(GameState(gameState))
        }
        roomState = Playing
      }
    }

    case Terminated(client) => {
      members -= client
      notifyAll(Members(memberNames))

      // Pause the game if a player left.
      if (players contains client) {
        players -= client
        notifyAll(Players(playerNames))
        if (roomState == Playing) {
          roomState = Paused
        }
      }

      // Destroy this room if all children are gone.
      if (members.size == 0) {
        context.stop(self)
      }
    }

    /* ServerMessages */
    case Move(mv) => {
      players.get(sender) map { idx =>
        if (roomState != Playing) {
          sender ! Message("The game hasn't started yet!")
        } else {
          move(gameState, idx, mv) match {
            case Success(newState) => {
              gameState = newState
              notifyAll(GameState(newState))
              if (gameEnd(newState)) {
                roomState = Lobby
              }
            }
            case Failure(e) =>
              sender ! Message(s"You've made a bad move: $e")
          }
        }
      }
    }
    case Chat(text) => {
      members.get(sender) map { name =>
        notifyAll(Message(s"$name: $text"))
      }
    }
    case ChangeRole(role) => {
      if (roomState == Playing) {
        sender ! Message("Cannot change roles in the middle of a game.")
      } else {
        if (role <= 0 || role > maxPlayers) {
          // Remove player if invalid number is given.
          players -= sender
          notifyAll(Players(playerNames))
        } else if (playersByIndex contains role) {
          sender ! Message("That role is unavailable.")
        } else {
          // Send the role update information.
          players += (sender -> role)
          notifyAll(Players(playerNames))
        }
      }
    }
    case ChangeName(name) => {
      members.get(sender) map { _ =>
        members += (sender -> name)
        notifyAll(Members(memberNames))
        notifyAll(Players(playerNames))
      }
    }
    case RequestUpdate(data) => {
      // TODO Request specific data...
      notifyAll(Members(memberNames))
      notifyAll(Players(playerNames))
      notifyAll(GameState(gameState))
    }
    case Restart => {
      members.get(sender) map { _ =>
        if (roomState == Lobby) {
          if (players.size == maxPlayers) {
            gameState = initState
            roomState = Playing
            notifyAll(GameState(gameState))
          } else {
            sender ! Message("Not enough players to start the game.")
          }
        } else {
          sender ! Message("Can't restart while still playing.")
        }
      }
    }
  }
}

// todo Switch to an FSM model for this.
/**
 * The GameRoom actor handles the logic for a single game room.
 * It adds and removes players, creates websockets, starts and stops the game, etc.
 */
trait GameRoom[State, Mov] extends Actor {
  import RoomState._

  /*
   * The following defines the public interface for this class.
   * Override these methods to implement room behavior.
   */

  /** The number of players needed to play the game. */
  def maxPlayers: Int

  /** How to convert a move from JSON input. */
  def moveFromJson(input: JsValue): Option[Mov]

  /** How to transform a state into JSON. */
  def stateToJson(input: State): JsValue

  /** Move from one action to another. */
  def move(state: State, idx: Int, mv: Mov): Try[State]

  /** Initial state of the game. */
  def initState: State

  /** Marker on the end of the game. */
  def gameEnd(state: State): Boolean

  case class Move(move: Mov) extends ServerMessage

  case class GameState(data: State) extends ClientMessage[State] {
    override def dataToJson = stateToJson(state)
  }

  def memberNames = members.keys.map(usernames)
  def playerNames = (0 until maxPlayers).map {i => 
    playersByIndex.get(i).map(usernames).getOrElse("")
  }

  def jsData(kind: String): JsValue = {
    val data: JsValue = kind match {
      case "members" => Json.arr(members.keys.map(usernames))
      case "players" => Json.toJson((0 until maxPlayers).map { i =>
        playersByIndex.get(i).map(usernames).getOrElse("")
      })
      case "gamestate" => stateToJson(state)
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

  case class MessageWrapper(uid: String, msg: ServerMessage)

  def serverMessage(uid: String, kind: String, data: JsValue): Option[MessageWrapper] = (kind match {
    case "update" => data.asOpt[Array[String]] map { x => RequestUpdate(x.toSet) }
    case "changerole" => data.asOpt[Int] map { ChangeRole }
    case "changename" => data.asOpt[String] map { ChangeName }
    case "move" => moveFromJson(data) map { Move }
    case "message" => data.asOpt[String] map { Chat }
    case "restart" => Some(Restart)
    case _ => None
  }) map { MessageWrapper(uid, _) }

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
          sendAll(jsData("gamestate"))
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
    case MessageWrapper(uid, Move(mv)) => {
      for (idx <- players.get(uid)) {
        if (roomState != Playing) {
          members(uid).push(jsMessage(s"The game hasn't started yet!"))
        } else {
          move(state, idx, mv) match {
            case Success(newState) => {
              state = newState
              sendAll(jsData("gamestate"))
              // Move back to our lobby if necessary.
              if (gameEnd(state)) {
                roomState = Lobby
              }
            }
            case Failure(e) =>
              members(uid).push(jsMessage(s"You've made a bad move: $e"))
          }
        }
      }
    }
    case MessageWrapper(uid, Chat(message)) => {
      sendAll(jsMessage(s"${usernames(uid)}: $message"))
    }
    case MessageWrapper(uid, ChangeRole(role)) => {
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
    case MessageWrapper(uid, ChangeName(name)) => {
      members.get(uid) map { channel =>
        usernames += (uid -> name)
        sendAll(jsData("players"))
        sendAll(jsData("members"))
      }
    }

    case MessageWrapper(uid, RequestUpdate(data)) => {
      for (kind <- data) {
        members(uid).push(jsData(kind))
      }
    }
    case MessageWrapper(uid, Restart) => {
      members.get(uid) map { channel =>
        // Can only start the game from the lobby
        if (roomState == Lobby) {
          if (players.size == maxPlayers) {
            state = initState
            roomState = Playing
            sendAll(jsData("gamestate"))
          }
          else {
            channel.push(jsMessage("Not enough players to start the game."))
          }
        } else {
          channel.push(jsMessage("Can't restart while still playing."))
        }
      }
    }
  }
}
