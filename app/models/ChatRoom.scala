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

object ChatRoom {
  implicit val timeout = Timeout(1.second)
  def join(room: ActorRef, username: String): scala.concurrent.Future[(Iteratee[JsValue,_], Enumerator[JsValue])] = {
    (room ? Join(username)).map {
      case Connected(enumerator) =>
        // Create an Iteratee to consume the feed
        val iteratee = Iteratee.foreach[JsValue] { event =>
          room ! Talk(username, (event \ "text").as[String])
        }.mapDone { _ =>
          room ! Quit(username)
        }

        (iteratee, enumerator)

      case CannotConnect(error) =>
        // Connection error
        val iteratee = Done[JsValue, Unit]((), Input.EOF)
        val enumerator = Enumerator[JsValue](JsObject(Seq("error" -> JsString(error)))).andThen(Enumerator.enumInput(Input.EOF))

        (iteratee, enumerator)
    }
  }
  
}

class ChatRoom(val id: String) extends Actor {
  var members = Set.empty[String]
  val (chatEnumerator, chatChannel) = Concurrent.broadcast[JsValue]


  def receive = {
    case Join(username) => {
      if(members.contains(username)) {
        sender ! CannotConnect("This username is already used")
      } else {
        members = members + username
        sender ! Connected(chatEnumerator)
        self ! NotifyJoin(username)
      }
    }

    case NotifyJoin(username) => {
      notifyAll("join", username, "has entered the room")
    }

    case Talk(username, text) => {
      notifyAll("talk", username, text)
    }

    case Quit(username) => {
      members = members - username
      notifyAll("quit", username, "has left the room")
      // TODO: destroy chatroom when all users have left.
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

case class Join(username: String)
case class Quit(username: String)
case class Talk(username: String, text: String)
case class NotifyJoin(username: String)

case class Connected(enumerator: Enumerator[JsValue])
case class CannotConnect(msg: String)
