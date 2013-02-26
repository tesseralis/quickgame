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

object TicTacToe {
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

class TicTacToe(val id: String) extends GameRoom {
  override def receive = super.receive orElse {
    case _ => 
  }
}

