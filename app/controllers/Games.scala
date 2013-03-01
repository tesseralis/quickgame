package controllers

import utils.GameType
import models._

import akka.actor.Props

object Games {
  /** Enumeration of the available games we have. */
  def values: Set[GameType] = Set(Chat, TicTacToe, ConnectFour)

  // Defines the mapping from the URI to the game
  def withName(s: String): GameType = s.toLowerCase match {
    case "chat" => Chat
    case "tictactoe" => TicTacToe
    case "connectfour" => ConnectFour
    case _ => throw new java.util.NoSuchElementException(s"Game $s not found.")
  }

  case object Chat extends GameType {
    override def view = views.html.chat.render
    override def props = Props[ChatRoom]
  }
  case object TicTacToe extends GameType {
    override def view = views.html.tictactoe.render
    override def props = Props[TicTacToeRoom]
  }
  case object ConnectFour extends GameType {
    override def view = views.html.connectfour.render
    override def props = Props[ConnectFourRoom]
  }
}
