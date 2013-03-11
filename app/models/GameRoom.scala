package models

import scala.util.{Try, Success, Failure}
import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import play.api.libs.json._
import play.api.libs.iteratee.{Iteratee, Concurrent}
import play.api.libs.concurrent.Execution.Implicits._

// Classes to handle the room state
object GameRoom {
  case class Join(name: Option[String])
}

object RoomState extends Enumeration {
  type RoomState = Value
  val Playing, Paused, Lobby = Value
}

trait GameRoom[State, Move] extends Actor {
  import RoomState._
  import GameRoom._

  implicit val timeout = Timeout(10.seconds)

  /*
   * The following defines the public interface for this class.
   * Override these methods to implement room behavior.
   */

  /** The number of players needed to play the game. */
  def maxPlayers: Int

  /** How to convert a move from JSON input. */
  def moveFromJson(input: JsValue): Option[Move]

  /** How to transform a state into JSON. */
  def stateToJson(input: State): JsValue

  /** Move from one action to another. */
  def move(state: State, idx: Int, mv: Move): Try[State]

  /** Initial state of the game. */
  def initState: State

  /** Return the name of the winner (-1 if a draw), or None if the game hasn't ended. */
  def winner(state: State): Option[Int]

  /* Private state variables (to be replaced with an FSM) */
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
  def otherNames = (members.keys.toSet -- players.keys).map(members(_)).toSeq
  def playerNames = (0 until maxPlayers).map {i => 
    playersByIndex.get(i).map(members).getOrElse("")
  }
  def memberNames = (playerNames, otherNames)

  /* Additional messages specific to states. */
  object GameMove extends AbstractMove[Move] {
    override def fromJson(data: JsValue) = moveFromJson(data)
  }
  object GameState extends AbstractState[State] {
    override def toJson(data: State) = stateToJson(data)
  }

  /**
   * Helper object to send the message to all children.
   */
  object all {
    def !(message: Any) { context.children.foreach(_ ! message) }
  }

  override def receive = {

    case Join(name) => {
      val client = context.actorOf(Props(new Client[JsValue]))
      context.watch(client)

      // Send the websocket to the sender
      (client ? Client.RequestWebsocket) pipeTo sender

      // Assign the name to the member
      members += (client -> name.getOrElse("user" + client.path.name))

      // Make a player if spots are available
      if (players.size < maxPlayers) {
        players += (client -> (0 until maxPlayers).indexWhere(!playersByIndex.contains(_)))
      }
      all ! Members(memberNames)
    }

    case Terminated(client) => {
      members -= client

      // Pause the game if a player left.
      if (players contains client) {
        players -= client
        if (roomState == Playing) {
          roomState = Paused
        }
      }
      all ! Members(memberNames)

      // Destroy this room if all children are gone.
      if (members.size == 0) {
        context.stop(self)
      }
    }

    /* ServerMessages */
    case GameMove(mv) => {
      players.get(sender) map { idx =>
        if (roomState != Playing) {
          sender ! Message("The game is not in session.")
        } else {
          move(gameState, idx, mv) match {
            case Success(newState) => {
              gameState = newState
              all ! GameState(newState)
              for (win <- winner(newState)) {
                all ! Message(if (win >= 0) s"${members(playersByIndex(win))} has won!"
                              else "The game is a draw.")
                roomState = Lobby
              }
            }
            case Failure(e) =>
              sender ! Message(e.getMessage)
          }
        }
      }
    }
    case Chat(text) => {
      members.get(sender) map { name =>
        all ! Message(s"$name: $text")
      }
    }
    case ChangeRole(role) => {
      if (roomState == Playing) {
        sender ! Message("Cannot change roles in the middle of a game.")
      } else {
        if (role < 0 || role >= maxPlayers) {
          // Remove player if invalid number is given.
          players -= sender
          sender ! Message("You have been removed as a player.")
          all ! Members(memberNames)
        } else if (players.contains(sender) && players(sender) == role) {
          sender ! Message(s"You are already that player!")
        } else if (playersByIndex contains role) {
          sender ! Message("That role is unavailable.")
        } else {
          // Send the role update information.
          players += (sender -> role)
          sender ! Message(s"You have changed your role.")
          all ! Members(memberNames)
        }
      }
    }
    case ChangeName(name) => {
      members.get(sender) map { _ =>
        members += (sender -> name)
        all ! Members(memberNames)
      }
    }
    case Update(x) => {
      all ! Members(memberNames)
      all ! GameState(gameState)
    }
    case Start(x) => {
      if (roomState != Playing) {
        if (players.size == maxPlayers) {
          if (roomState = Lobby) {
            gameState = initState
          }
          roomState = Playing
          all ! Message("The game has started!")
          all ! GameState(gameState)
        } else {
          sender ! Message("Not enough players to start/resume the game.")
        }
      } else {
        sender ! Message("The game is already playing.")
      }
    }
    case Stop(x) => {
      if (roomState != Lobby) {
        roomState = Lobby
        all ! Message("The game has been cancelled! Boo.")
      } else {
        sender ! Message("The game is not playing right now.")
      }
    }
  }
}
