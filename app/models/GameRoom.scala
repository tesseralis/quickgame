package models

import akka.actor.Actor

import play.api.Logger

class GameRoom extends Actor {
  override def receive = {
    case JoinRoom(uid) =>
    case QuitRoom(uid) =>
    case ChooseRole(uid, role) =>
  }
}

case class JoinRoom(uid: String)
case class QuitRoom(uid: String)
case class ChooseRole(uid: String, role: Int)
