package models

import scala.util.Success
import play.api.libs.json.{JsValue, JsString}


class ChatRoom extends GameRoom[String, String] {
  override def maxPlayers = Int.MaxValue

  override def moveFromJson(js: JsValue) = (js\"text").asOpt[String]

  override def stateToJson(state: String) = JsString(state)

  override def move(state: String, idx: Int, mv: String) = Success(mv)

  override def initState = ""

  override def winner(state: String) = None
}
