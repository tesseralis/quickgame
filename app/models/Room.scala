package models

import akka.actor._

import common.models.Game1

object Room {
  /** Message sent by a user wanting to join. */
  case class Join(name: String)
  
  // Room messages
  // NOTE You cannot have CamelCased names or the messages won't work
  // for some stupid reason...
  case class ChangeName(name: String)
  case object Update

  /** Stores information about each member. */
  case class MemberData(name: String)

  sealed trait State
  case object Idle extends State
  case object Playing extends State

  case class Data[GS](members: Map[ActorRef, MemberData], gamestate: GS)
}


/**
 * Represents a room that clients can connect to, to play games.
 * @param game The game definition used for this room
 */
class Room[GS](game: Game1[GS, _]) extends Actor with FSM[Room.State, Room.Data[GS]] {
  import Room._
  startWith(Idle, Data(Map.empty, game.init))

  when(Idle) {

    case Event(Join(name), Data(members, gamestate)) => 
      context.watch(sender)
      stay using Data(members + (sender -> MemberData(name)), gamestate)

    case Event(Terminated(client), Data(members, gamestate)) =>
      stay using Data(members - client, gamestate)

    case Event(ChangeName(name), Data(members, gamestate)) =>
      stay using Data(members + (sender -> MemberData(name)), gamestate)

    case Event(Update, data) =>
      stay replying data 

  }

  when (Playing)(FSM.NullFunction)

  initialize
}
