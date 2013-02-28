package models

import scala.util.Success
import play.api.libs.json.{JsValue, JsString}

class ChatRoom extends GameRoom[String, String] {
  override def maxPlayers = Int.MaxValue

  override def parseMove(js: JsValue) = (js\"text").asOpt[String]

  override def encodeState(state: String) = JsString(state)

  override def move(state: String, idx: Int, mv: String) = Success(mv)

  override def initState = ""
}
