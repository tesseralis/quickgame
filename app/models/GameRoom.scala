package models

import akka.actor.Actor

import play.api.Logger

class GameRoom extends Actor {
  override def receive = {
    case msg => Logger.debug(s"Received $msg")
  }
}
