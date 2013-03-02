package models

import akka.actor._
import play.api.libs.json._
import play.api.libs.iteratee.{Iteratee, Concurrent}

class Client(moveFromJson: JsValue => Option[AbstractMove[_]]) extends Actor {
  val (enumerator, channel) = Concurrent.broadcast[JsValue]
  val iteratee = Iteratee.foreach[JsValue] { json =>
    ServerMessage.fromJson(json)(moveFromJson).foreach { msg =>
      context.parent ! msg
    }
  } mapDone { _ => context.stop(self) }

  override def receive = {
    // Pass the websocket up
    case RequestWebsocket =>
      sender ! (iteratee, enumerator)

    case msg: JsValue => 
      channel.push(msg)
  }
}

case object RequestWebsocket
