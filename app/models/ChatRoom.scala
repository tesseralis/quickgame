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
  implicit val timeout = Timeout(10.second)

  def iteratee(room: ActorRef, username: String): Iteratee[JsValue, _] =
    Iteratee.foreach[JsValue] { event =>
      room ! Talk(username, (event \ "text").as[String])
    } mapDone { _ =>
      room ! Quit(username)
    }
}

class ChatRoom(val id: String) extends GameRoom {

}
