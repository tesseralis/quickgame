package models

import akka.actor._
import play.api.libs.json._
import play.api.libs.iteratee.{Iteratee, Concurrent}

class Client(messageFromJson: (String, JsValue) => Option[ServerMessage]) extends Actor {
  val (enumerator, channel) = Concurrent.broadcast[JsValue]
  val iteratee = Iteratee.foreach[JsValue] { json =>
    for (kind <- (json\"kind").asOpt[String]; msg <- messageFromJson(kind, json\"data")) {
      context.parent ! msg
    }
  } mapDone { _ => context.stop(self) }

  override def receive = {
    // Pass the websocket up
    case Join(name) =>
      sender ! (iteratee, enumerator)

    case msg: ClientMessage[_] =>
      channel.push(msg.toJson)
  }
}
