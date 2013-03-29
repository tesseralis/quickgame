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
  case class Chat(text: String)

  // Room replies
  case class Message(text: String)

  sealed trait Role
  case object Spectator extends Role
  case class Player(i: Int) extends Role
  /** Stores information about each member. */
  case class MemberData(name: String, role: Role)

  sealed trait State
  case object Idle extends State
  case object Playing extends State

  case class Data[GS](members: Map[ActorRef, MemberData], gamestate: GS)

  def nextAvailableRole[A](limit: Int, members: Map[A, MemberData]): Role = {
    val indices = members.collect {
      case (_, MemberData(_, Player(i))) => i
    }.toSet

    (0 until limit).find(!indices.contains(_)).map {
      Player(_)
    }.getOrElse {
      Spectator
    }
  }

  def broadcast[A](members: Map[ActorRef, A], message: Any) {
    for ((member, _) <- members) {
      member ! message
    }
  }
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
      val role = nextAvailableRole(game.numPlayers, members)
      stay using Data(members + (sender -> MemberData(name, role)), gamestate)

    case Event(Terminated(client), Data(members, gamestate)) =>
      stay using Data(members - client, gamestate)

    case Event(ChangeName(name), Data(members, gamestate)) =>
      members.get(sender).map { memdata =>
        val memdata1 = memdata.copy(name = name)
        stay using Data(members + (sender -> memdata1), gamestate)
      } getOrElse {
        stay replying Message("You are not in this room.")
      }

    case Event(Update, data) =>
      stay replying data 

    case Event(Chat(text), Data(members, _)) =>
      broadcast(members, Message(text))
      stay
  }

  when (Playing)(FSM.NullFunction)

  initialize
}
