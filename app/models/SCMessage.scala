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

object Members extends SCMessage[(Seq[String], Seq[String])] {
  override def kind = "members"
  override def toJson(data: (Seq[String], Seq[String])) =
    Json.obj(
      "players" -> JsArray(data._1.map(JsString)),
      "members" -> JsArray(data._2.map(JsString))
    )
}
object Message extends SCMessage[String] {
  override def kind = "message"
  override def toJson(data: String) = JsString(data)
}
abstract class AbstractState[S] extends SCMessage[S] {
  final override def kind = "gamestate"
}
