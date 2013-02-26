package models

/**
 * This class contains an enumeration on the games we have available.
 */

object GameType {
  def values: Set[GameType] = Set(Chat, Tictactoe)
  def withName(s: String): GameType = s.toLowerCase match {
    case "chat" => Chat
    case "tictactoe" => Tictactoe
    case _ => throw new java.util.NoSuchElementException(s"Game $s not found.")
  }
}
trait GameType
case object Chat extends GameType
case object Tictactoe extends GameType
