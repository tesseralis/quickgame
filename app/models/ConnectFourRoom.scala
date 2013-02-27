package models

import scala.util.{Try, Success, Failure}
import akka.actor.Actor

import play.api.Logger
import play.api.libs.iteratee.{Iteratee, Concurrent}
import play.api.libs.json.{Json, JsValue}

object ConnectFourRoom {
  type Pos = Int
  type Player = Int
  type Board = Map[(Int, Int), Player]

  trait State {
    def board: Board
    def move(player: Player, pos: Pos): Try[State] = this match {
      case s @ GameStart(board, currentPlayer) => Try {
        s
      }
      case GameEnd(_, _) => Failure(new Exception("The game is completed."))
    }
  }

  case class GameStart(board: Board, currentPlayer: Player) extends State
  case class GameEnd(board: Board, player: Player) extends State

  case class Move(username: String, move: Pos)
}

import ConnectFourRoom._

class ConnectFourRoom extends Actor {
  var state: State = GameStart(Map.empty withDefaultValue -1, 0)
  var players = Map[String, Int]()
  var currentNumPlayers = 0
  val (chatEnumerator, chatChannel) = Concurrent.broadcast[JsValue]

  def iteratee(username: String): Iteratee[JsValue, _] = Iteratee.foreach[JsValue] { event =>
    self ! Move(username, (event\"col").as[Int])
  } mapDone { _ => self ! Quit(username) }

  def sendState(state: State) {
    val msg = Json.obj()
    chatChannel.push(msg)
  }

  override def receive = {
    case Join(username) =>
      if (players contains username) {
        sender ! CannotConnect("This username is already used")
      } else if (currentNumPlayers >= 2) {
        sender ! CannotConnect("Too many players!")
      } else {
        players += (username -> currentNumPlayers)
        currentNumPlayers += 1
        sender ! Connected(iteratee(username), chatEnumerator)
      }
    case Quit(username) =>
      if (players contains username) {
       players -= username 
      }

    case Move(username, move) =>
      Logger.debug(username + " " + move)
      players.get(username) map { playerNumber =>
        state.move(playerNumber, move) match {
          case Failure(e) => {
            Logger.debug(s"Bad move $move made by $username: $e")
          }
          case Success(newState) => {
            Logger.debug(s"Valid move $move made by $username")
            state = newState
            sendState(state)
          }
        }
      } getOrElse {
        Logger.debug(s"Unknown player $username")
      }
  }

}
