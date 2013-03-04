# quickgame

Quick boardgame platform a la collabedit.

## Websocket Protocol
Quickgame uses JSON data passed through websockets to communicate between the client and server.
Each message is a JSON object of the form ``{"kind": [KIND], "data": [DATA]}``.
Below lists all currently supported message types, in the form [KIND]: [DATA]

### Server-to-client
* members: A list of players (in order) and a list of other members.
* gamestate: Object encoding of the game state (specific to each game)
* message: String message from server

### Client-to-server
* update: No data.
  Sent by the client in order to force the server to send data to the client.
* changerole: Number specifying the role to take.
  Sent by the client to request a change of player role.
* changename: String specifying the name to take.
  Sent by the client to request a change in name
  (multiple people in the room can have the same name).
* move: Object encoding of a single move (specific to each game).
  Sent by the client to request a move.
* restart: No data.
  Request that the game be restarted.
* chat: String chat message from the client.

## TODO
### Important
This is a list of things I (Nathan) believe to be necessary for our MVP.
* Ability to start a new game in a gameroom
* Display current players and people in the room
* Display messages sent from the server
* Change role in client

### Not Important
* Look up FSM, Dispatchers, and other Akka stuff to see if we can make things more modular
