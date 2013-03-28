package test

import org.scalatest.{FlatSpec, OneInstancePerTest}

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestFSMRef, ImplicitSender}

import common.models.Game1

import models.Room
import models.Room._

/*
 * Define a simple game to test the room on.
 */
object MockGame extends Game1[Int, Int] {
  override def numPlayers = 1
  override def init = 0
  override def isFinal(s: State) = false
  override def transition(s: State, p: Player, m: Move) =
    util.Success(0)
}

class RoomSpec(_system: ActorSystem) extends TestKit(_system) 
    with FlatSpec
    with OneInstancePerTest {

  def this() = this(ActorSystem("RoomSpec"))

  val room = TestFSMRef(new Room(MockGame))

  behavior of "A game room"

  it should "start properly" in {
    assert(room.stateData.members.isEmpty)
    info("Starts with no members.")

    assert(room.stateData.gamestate === MockGame.init)
    info("Starts with the initial game state.")

    assert(room.stateName === State.Idle)
    info("Starts in the Idle state.")
  }
}
