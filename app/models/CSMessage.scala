package models

import play.api.libs.json.JsValue

/**
 * Trait providing pattern matchers for JSON messages from
 * the client to the server.
 */
trait CSMessage[A] {
  protected def fromJson(data: JsValue): Option[A]
  protected def kind: String

  final def unapply(js: JsValue): Option[A] = {
    for {
      k <- (js\"kind").asOpt[String]
      if k == kind
      data <- fromJson(js\"data")
    } yield data
  }
}

object ChangeRole extends CSMessage[Int] {
  override def kind = "changerole"
  override def fromJson(data: JsValue) = data.asOpt[Int]
}
object ChangeName extends CSMessage[String] {
  override def kind = "changename"
  override def fromJson(data: JsValue) = data.asOpt[String]
}
object Restart extends CSMessage[Unit] {
  override def kind = "restart"
  override def fromJson(data: JsValue) = Some(Unit)
}
object Update extends CSMessage[Unit] {
  override def kind = "update"
  override def fromJson(data: JsValue) = Some(Unit)
}
object Chat extends CSMessage[String] {
  override def kind = "update"
  override def fromJson(data: JsValue) = data.asOpt[String]
}
abstract class AbstractMove[M] extends CSMessage[M] {
  override def kind = "move"
}
