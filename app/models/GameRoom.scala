package models

import scala.util.{Try, Success, Failure}

import akka.actor.Actor

import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.libs.iteratee.{Iteratee, Concurrent}

trait GameRoom[State, Mov] extends Actor {
  case class Move(user: String, move: Mov)

  // The number of players needed to play the game
  def maxPlayers: Int

  // How to parse a JSON move
  def parseMove(input: JsValue): Mov

  def encodeState(input: State): JsValue

  def move(state: State, idx: Int, mv: Mov): Try[State]

  def initState: State

  private[this] def notifyAll(kind: String, user: String) {
    val msg = Json.obj(
      "kind" -> kind,
      "user" -> user,
      "state" -> encodeState(state),
      "members" -> Json.arr(members)
      // TODO List players
    )
    channel.push(msg)
  }

  private[this] def iteratee(username: String) = Iteratee.foreach[JsValue] { move =>
    self ! Move(username, parseMove(move))
  } mapDone { _ => self ! Quit(username) }

  // The current members of this room
  var members = Set[String]()
  // A map of players to their position in the game
  var players = Map[String, Int]()

  var state = initState
  // TODO: Have one for each player and store in members as a map
  val (enumerator, channel) = Concurrent.broadcast[JsValue]

  override def receive = {
    case Join(username) => {
      if (members contains username) {
        sender ! CannotConnect("This username is already in use.")
      } else {
        if (players.size < maxPlayers) {
          players += (username -> (0 until maxPlayers).indexWhere(!players.values.toSet.contains(_)))
        }
        members += username
        notifyAll("join", username)
        sender ! Connected(iteratee(username), enumerator)
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
        }
      }
    }
  }
}
