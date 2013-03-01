package models

import scala.util.Success
import play.api.libs.json.{JsValue, JsString}

case class ChatState(text: String) extends GameState {
  override def gameEnd = false
}

class ChatRoom extends GameRoom[ChatState, String] {
  override def maxPlayers = Int.MaxValue

  override def parseMove(js: JsValue) = (js\"text").asOpt[String]

  override def encodeState(state: ChatState) = JsString(state.text)

  override def move(state: ChatState, idx: Int, mv: String) = Success(ChatState(mv))

  override def initState = ChatState("")
}
