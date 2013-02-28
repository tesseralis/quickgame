quickgame
=========

Quick boardgame platform a la collabedit.

## Websocket Protocol
Quickgame uses JSON data passed through websockets to communicate between the client and server.
Below is a list of possible key-value pairs. For example, the server can send:
    {
      "members": ["Nathan", "Sal", "Andrew"], 
      "players": ["Nathan", "Sal"],
      "message": "Andrew joined the room"
    }
The client is expected to update its members and players, and optionally flash the message.

### Server-to-client
* members: Array of current members in the room
* players: Array of current players (sorted by role)
* gamestate: Object encoding of the game state (specific to each game)
* message: String message from server

### Client-to-server
* update: Returns sublist of [members, players, gamestate].
  Sent by the client in order to force the server to send data to the client.
* changerole: Number specifying the role to take.
  Sent by the client to request a change of player role.
* move: Object encoding of a single move (specific to each game).
  Sent by the client to request a move.
