package models

import scala.reflect.ClassTag

import akka.actor._
import play.api.libs.json._
import play.api.libs.iteratee.{Iteratee, Concurrent}

class Client[A : ClassTag] extends Actor {
  val (enumerator, channel) = Concurrent.broadcast[A]
  val iteratee = Iteratee.foreach[A] { json =>
    context.parent ! json
  } mapDone { _ => context.stop(self) }

  override def receive = {
    // Pass the websocket up
    case RequestWebsocket =>
      sender ! (iteratee, enumerator)

    case msg: A => 
      channel.push(msg)
  }
}

case object RequestWebsocket
