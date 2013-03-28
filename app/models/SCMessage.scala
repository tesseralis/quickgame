package models

import play.api.libs.json._

/**
 * Trait providing JSON constructors for Client-to-Server messages.
 */
sealed abstract class SCMessage[A : Writes](kind: String) {
  final def apply(data: A): JsValue = Json.obj(
    "kind" -> kind,
    "data" -> data
  )
}
object SCMessage {
  implicit object MembersWrites extends Writes[(Seq[String], Seq[String])] {
    override def writes(data: (Seq[String], Seq[String])) =
      Json.obj(
        "players" -> JsArray(data._1.map(JsString)),
        "others" -> JsArray(data._2.map(JsString))
      )
  }

  object Members extends SCMessage[(Seq[String], Seq[String])]("members")
  object Message extends SCMessage[String]("message")
  object Room extends SCMessage[String]("room")
  abstract class AbstractState[S : Writes] extends SCMessage[S]("gamestate")
}

