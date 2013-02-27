package models

import akka.actor._
import scala.concurrent.duration._

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import akka.util.Timeout
import akka.pattern.ask

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._


class ChatRoom(val id: String) extends Actor {
  var members = Set.empty[String]
  val (chatEnumerator, chatChannel) = Concurrent.broadcast[JsValue]

  def iteratee(username: String): Iteratee[JsValue, _] =
    Iteratee.foreach[JsValue] { event =>
      Logger.debug(event.toString)
      self ! Talk(username, (event \ "text").as[String])
    } mapDone { _ =>
      self ! Quit(username)
    }

  def receive = {
    case Join(username) => {
      if(members.contains(username)) {
        sender ! CannotConnect("This username is already used")
      } else {
        members = members + username
        sender ! Connected(iteratee(username), chatEnumerator)
        notifyAll("join", username, "has entered the room")
      }
    }

    case Talk(username, text) => {
      notifyAll("talk", username, text)
    }

    case Quit(username) => {
      members = members - username
      notifyAll("quit", username, "has left the room")
      // TODO: destroy gameroom when all users have left.
    }
  }

  def notifyAll(kind: String, user: String, text: String) {
    val msg = JsObject(
      Seq(
        "kind" -> JsString(kind),
        "user" -> JsString(user),
        "message" -> JsString(text),
        "members" -> JsArray(
          members.toList.map(JsString)
        )
      )
    )
    chatChannel.push(msg)
  }
}
