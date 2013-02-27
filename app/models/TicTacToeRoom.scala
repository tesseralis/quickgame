package models

import scala.util.{Try, Success, Failure}

import akka.actor._
import scala.concurrent.duration._

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import akka.util.Timeout
import akka.pattern.ask

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

object TicTacToeRoom {
  implicit val timeout = Timeout(1.second)

  def iteratee(room: ActorRef, username: String): Iteratee[JsValue, _] =
    Iteratee.foreach[JsValue] { event => 
      Logger.debug(event.toString)
      (event \ "kind").as[String] match {
        case "talk" => room ! Talk(username, (event \ "text").as[String])
        case "turn" => {
          Logger.debug((event\"row").as[Int] + " " + (event\"col").as[Int])
          room ! TicTacToeTurn(username, ((event \ "row").as[Int], (event \ "col").as[Int]))
        }
      }
    } mapDone { _ =>
      room ! Quit(username)
    }
}

class TicTacToeRoom(val id: String) extends Actor {
  var gameState = TicTacToeModel.empty
  var players = Map[String,Int]()
  var currentNumPlayers = 0
  val (chatEnumerator, chatChannel) = Concurrent.broadcast[JsValue]

  def sendState(state: TicTacToeModel) {
    val (stateString, player) = state match {
      case Turn(_, p) => ("turn", p)
      case Win(_, p) => ("win", p)
      case Draw(_) => ("draw", -1)
    }
    val jsonBoard = JsArray(for (i <- 0 until 3) yield {
      JsArray(for (j <- 0 until 3) yield {
        JsNumber(BigDecimal(state.board.get((i, j)).getOrElse(-1)))
      })
    })
    val msg = Json.obj(
      "kind" -> stateString,
      "player" -> player,
      "board" -> jsonBoard
    )
    Logger.debug(msg.toString)
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
        sender ! Connected(chatEnumerator)
      }

    case Quit(username) =>
      if (players contains username) {
       players -= username 
      }

    case TicTacToeTurn(username, move) =>
      Logger.debug(username + " " + move)
      players.get(username) map { playerNumber =>
        gameState.move(playerNumber, move) match {
          case Failure(e) => {
            Logger.debug(s"Bad move $move made by $username: $e")
          }
          case Success(newState) => {
            Logger.debug(s"Valid move $move made by $username")
            gameState = newState
            sendState(gameState)
          }
        }
      } getOrElse {
        Logger.debug(s"Unknown player $username")
      }
  }
}

case class TicTacToeTurn(username: String, move: (Int,Int))
