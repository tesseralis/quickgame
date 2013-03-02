package models
import play.api.libs.json._

/**
 * Message representation of a message sent from the server to the client.
 */
sealed trait ClientMessage[A] {
  def data: A
  def dataToJson: JsValue
  def kind: String
  
  def toJson: JsValue = Json.obj(
    "kind" -> JsString(kind),
    "data" -> dataToJson
  )
}

case class Members(data: Seq[String]) extends ClientMessage[Seq[String]] {
  override def dataToJson = JsArray(data.map(JsString))
  override def kind = "members"
}
case class Players(data: Seq[String]) extends ClientMessage[Seq[String]] {
  override def dataToJson = JsArray(data.map(JsString))
  override def kind = "players"
}
case class Message(data: String) extends ClientMessage[String] {
  override def dataToJson = JsString(data)
  override def kind = "message"
}
abstract class AbstractGameState[S](state: S) extends ClientMessage[S]
