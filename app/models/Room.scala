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

  type Members = Map[ActorRef, MemberData]

  case class Data[GS](members: Members, gamestate: GS)

  def nextAvailableRole(limit: Int, members: Members): Role = {
    val indices = members.collect {
      case (_, MemberData(_, Player(i))) => i
    }.toSet

    (0 until limit).find(!indices.contains(_)).map {
      Player(_)
    }.getOrElse {
      Spectator
    }
  }

  def broadcast(members: Members, msg: Any) {
    for ((member, _) <- members) {
      member ! msg
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
      context.watch(sender) // We'll know when the member quits
      val role = nextAvailableRole(game.numPlayers, members)
      // Add to the list of members
      val members1 = members + (sender -> MemberData(name, role))
      broadcast(members1, members1)
      stay using Data(members1, gamestate)

    case Event(Terminated(client), Data(members, gamestate)) =>
      val members1 = members - client
      broadcast(members1, members1)
      stay using Data(members1, gamestate)

    case Event(ChangeName(name), Data(members, gamestate)) =>
      members.get(sender).map { memdata =>
        val members1 = members + (sender -> memdata.copy(name = name))
        broadcast(members1, members1)
        stay using Data(members1, gamestate)
      } getOrElse {
        stay replying Message("You are not in this room.")
      }

    case Event(Update, Data(members, gamestate)) =>
      stay replying (members) replying (gamestate)

    case Event(Chat(text), Data(members, _)) =>
      broadcast(members, Message(text))
      stay
  }

  when (Playing)(FSM.NullFunction)

  initialize
}
