package test

import org.scalatest._

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.testkit.{TestKit, TestActorRef, TestFSMRef, ImplicitSender}

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

class MockClient(val room: ActorRef) extends Actor {
  def receive = {
    case msg => room ! msg
  }
}

class RoomSpec(_system: ActorSystem) extends TestKit(_system) 
    with WordSpec
    with BeforeAndAfterAll
    with OneInstancePerTest {

  def this() = this(ActorSystem("RoomSpec"))

  val room = TestFSMRef(new Room(MockGame))

  override def afterAll {
    system.shutdown()
  }

  "A game room" should {

    "start properly" in {
      assert(room.stateData.members.isEmpty)
      info("Starts with no members.")

      assert(room.stateData.gamestate === MockGame.init)
      info("Starts with the initial game state.")

      assert(room.stateName === State.Idle)
      info("Starts in the Idle state.")
    }

    // TODO Make these shared tests between states

    "add and remove members" in {
      val client = TestActorRef(new MockClient(room))
      client ! Join("Nathan")
      assert(room.stateData.members.size === 1)
      info("Successfully joined the room.")

      client.stop()
      assert(room.stateData.members.size === 0)
      info("Successfully left the room.")
    }

    "allow members to change their name" in {
      val client = TestActorRef(new MockClient(room))
      client ! Join("Nathan")
      assert(room.stateData.members(client).name === "Nathan")
      client ! ChangeName("Sal")
      assert(room.stateData.members(client).name === "Sal")
    }

    "respond to update requests" is (pending)
  }

  "A game room (when idle)" should {
  
    "add the first members as players" is (pending)

    "allow members to freely change player roles" is (pending)

    "allow members to chat" is (pending)

    "not start the game when there are not enough players" is (pending)

    "start the game when the command is called and there are enough players" is (pending)

    "not allow any other commands" is (pending)

  }

  "A game room (when playing)" should {

    "let a player make valid move" is (pending)

    "not allow a spectator to make a move" is (pending)

    "not allow a player to make an invalid move" is (pending)

    "pause the game if a player leaves" is (pending)
  }

  "A game room (when paused)" should {

    "resume when the start command is set" is (pending)

    "not allow any moves" is (pending)

  }

}
