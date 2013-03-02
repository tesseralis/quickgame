package models

import scala.reflect.ClassTag

import akka.actor._
import play.api.libs.json._
import play.api.libs.iteratee.{Iteratee, Concurrent}

object Client {
  /** Object to send to poll the client for its websocket connection. */
  case object RequestWebsocket
}

/**
 * Actor representing a WebSocket client.
 * Simply passes sent messages to the websocket and
 * returns incoming messages to its parent.
 */
class Client[A : ClassTag] extends Actor {
  import Client._

  /** The channel is the input stream into the websocket.
   * Pass the enumerator out to allow sending messages from
   * the client. 
   */
  val (enumerator, channel) = Concurrent.broadcast[A]

  /** This socket just sends all messages to its parent. */
  val iteratee = Iteratee.foreach[A] { json =>
    context.parent ! json
  } mapDone { _ => context.stop(self) }

  override def receive = {
    case msg: A => channel.push(msg)
    case RequestWebsocket => sender ! (iteratee, enumerator)
  }
}

