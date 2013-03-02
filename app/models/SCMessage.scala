package models

import play.api.libs.json._

/**
 * Trait providing JSON constructors for Client-to-Server messages.
 */
sealed trait SCMessage[A] {
  protected def toJson(data: A): JsValue
  protected def kind: String

  final def apply(data: A): JsValue = Json.obj(
    "kind" -> JsString(kind),
    "data" -> toJson(data)
  )
}

object Members extends SCMessage[Seq[String]] {
  override def toJson(data: Seq[String]) = JsArray(data.map(JsString))
  override def kind = "members"
}
object Players extends SCMessage[Seq[String]] {
  override def toJson(data: Seq[String]) = JsArray(data.map(JsString))
  override def kind = "players"
}
object Message extends SCMessage[String] {
  override def toJson(data: String) = JsString(data)
  override def kind = "message"
}
abstract class AbstractGameState[S] extends SCMessage[S] {
  final override def kind = "gamestate"
}
