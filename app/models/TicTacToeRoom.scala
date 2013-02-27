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
      (event \ "kind").as[String] match {
        case "talk" => room ! Talk(username, (event \ "text").as[String])
        case "turn" => room ! TicTacToeTurn(username, ((event \ "row").as[Int], (event \ "col").as[Int]))
      }
    } mapDone { _ =>
      room ! Quit(username)
    }
}

class TicTacToeRoom(val id: String) extends GameRoom {
  var gameBoard = TicTacToeModel.empty
  var players = Map[String,Int]()
  var currentPlayer = 0
  def ticTacToeMessage : PartialFunction[Any, Unit] = {

    case join @ Join(username) =>
      super.receive(join)
      var possible = players.values.toSet diff Set(0,1)
      if (possible.size > 0) {
        players += (username -> possible.head)
      }

    case quit @ Quit(username) =>
      super.receive(quit)
      if(players contains username) {
       players -= username 
      }

    case TicTacToeTurn(username, move) =>
      Logger.debug(username + " " + move)
      if(players contains username) {
        val playerNumber = players(username)
        if(playerNumber == currentPlayer) {
          val newBoard = gameBoard.move(move)
          newBoard match {
            case Success(v) =>
              gameBoard = v
              v match {
                case Win(_,_) =>
                  notifyAll("status",username,username + " has won!")
                case Draw(_) => 
                  notifyAll("status",username,"The game is a draw.")
                case _ =>
                  val jsonBoard = JsArray(for (i <- 0 until 3) yield {
                    JsArray(for (j <- 0 until 3) yield {
                      JsString(gameBoard.board.get((i, j)).map((x: Int) => x.toString).getOrElse(""))
                    })
                  })
                  val msg = JsObject(
                    Seq(
                      "kind" -> JsString("state"),
                      "user" -> JsString(username),
                      "message" -> JsString("This is the current state of the game."),
                      "members" -> JsArray(members.toList.map(JsString)),
                      "board" -> jsonBoard
                    )
                  )
                  chatChannel.push(msg)
              }
            case Failure(e) =>  
              notifyAll("oops",username,username + " made a silly move")
          }
          
        }
      }

  }
  override def receive = ticTacToeMessage orElse super.receive
}

case class TicTacToeTurn(username: String, move: (Int,Int))
