package models

import play.api.libs.json.JsValue

object ServerMessage {
  def fromJson(js: JsValue)(moveFromJson: JsValue => Option[AbstractMove[_]]): Option[ServerMessage] = {
    val data = js\"data"
    (js\"kind").asOpt[String] flatMap {
      case "update" => data.asOpt[Array[String]] map { x => RequestUpdate(x.toSet) }
      case "changerole" => data.asOpt[Int] map { ChangeRole }
      case "changename" => data.asOpt[String] map { ChangeName }
      case "move" => moveFromJson(data)
      case "message" => data.asOpt[String] map { Chat }
      case "restart" => Some(Restart)
      case _ => None
    }
  }
}

/** Internal representation of a message sent from the client to the server. */
sealed trait ServerMessage
case class ChangeRole(role: Int) extends ServerMessage
case class ChangeName(name: String) extends ServerMessage
case object Restart extends ServerMessage
case class RequestUpdate(data: Set[String]) extends ServerMessage
case class Chat(text: String) extends ServerMessage
abstract class AbstractMove[M](move: M) extends ServerMessage
