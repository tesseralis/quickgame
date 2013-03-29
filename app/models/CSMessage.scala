package models

import play.api.libs.json._

// TODO This is really hacky. Is there another Null class we can use?
object CSMessage {
  implicit object UnitReads extends Reads[Unit] {
    override def reads(json: JsValue) = JsSuccess(Unit)
  }
  object ChangeRole extends CSMessage[Int]("changerole")
  object ChangeName extends CSMessage[String]("changename")
  object Start extends CSMessage[Unit]("start")
  object Stop extends CSMessage[Unit]("stop")
  object Update extends CSMessage[Unit]("update")
  object Chat extends CSMessage[String]("chat")
  abstract class AbstractMove[M : Reads] extends CSMessage[M]("move")
}

/**
 * Trait providing pattern matchers for JSON messages from
 * the client to the server.
 */
sealed abstract class CSMessage[A : Reads](kind: String) {
  final def unapply(js: JsValue): Option[A] = {
    for {
      k <- (js\"kind").asOpt[String]
      if k == kind
      data <- (js\"data").asOpt[A]
    } yield data
  }
}

