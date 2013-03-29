package models

import akka.actor._

import common.models.Game1

object Room {
  /** Message sent by a user wanting to join. */
  case class Join(name: String)
  
  // Room messages
  case class Changename(name: String)

  case class MemberData(name: String)

  object State extends Enumeration { 
    val Idle, Playing = Value 
  }
  type State = State.Value

  case class Data[GS](members: Map[ActorRef, MemberData], gamestate: GS)
}

import Room._
import Room.State._

/**
 * Represents a room that clients can connect to, to play games.
 * @param game The game definition used for this room
 */
class Room[GS](game: Game1[GS, _]) extends Actor with FSM[State, Data[GS]] {
  startWith(Idle, Data(Map.empty, game.init))

  when(Idle) {

    case Event(Join(name), Data(members, gamestate)) => 
      context.watch(sender)
      stay using Data(members + (sender -> MemberData(name)), gamestate)

    case Event(Terminated(client), Data(members, gamestate)) =>
      stay using Data(members - client, gamestate)

    case Event(Changename(name), data) =>
      stay using data.copy(members = data.members + (sender -> MemberData(name)))

  }

  initialize
}
