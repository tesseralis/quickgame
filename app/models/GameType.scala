package models

/**
 * This class contains an enumeration on the games we have available.
 */

object GameType {
  def values: Set[GameType] = Set(Chat, Tictactoe, Connectfour)
  def withName(s: String): GameType = s.toLowerCase match {
    case "chat" => Chat
    case "tictactoe" => Tictactoe
    case "connectfour" => Connectfour
    case _ => throw new java.util.NoSuchElementException(s"Game $s not found.")
  }
}
sealed trait GameType
case object Chat extends GameType
case object Tictactoe extends GameType
case object Connectfour extends GameType
