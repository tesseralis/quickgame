package models

import scala.util.Success
import play.api.libs.json.{JsValue, JsString}

case class ChatState(text: String) extends GameState[String, ChatState] {
  override def gameEnd = false
  override def move(i: Int, move: String) = Success(ChatState(move))
  override def toJson = JsString(text)
}

class ChatRoom extends GameRoom[String, ChatState] {
  override def maxPlayers = Int.MaxValue

  override def parseMove(js: JsValue) = (js\"text").asOpt[String]

  override def initState = ChatState("")
}
