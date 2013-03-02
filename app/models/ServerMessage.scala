package models

/** Internal representation of a message sent from the client to the server. */
sealed trait ServerMessage
case class ChangeRole(role: Int) extends ServerMessage
case class ChangeName(name: String) extends ServerMessage
case object Restart extends ServerMessage
case class RequestUpdate(data: Set[String]) extends ServerMessage
case class Chat(text: String) extends ServerMessage
abstract class AbstractMove[M](move: M) extends ServerMessage

