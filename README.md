# quickgame

Quick boardgame platform a la collabedit.

## Websocket Protocol
Quickgame uses JSON data passed through websockets to communicate between the client and server.
Each message is a JSON object of the form ``{"kind": [KIND], "data": [DATA]}``.
Below lists all currently supported message types, in the form [KIND]: [DATA]

### Server-to-client
* members: {players, others}. A list of players (in order) and a list of other members.
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
* start: No data. Request the game be started or resumed.
* stop: No data. Request that the game be stopped so a new one can begin.
* chat: String chat message from the client.

## TODO
### Client-side
* Changing usernames is unintuitive; there needs to be a button.
* The scrolling on the chat window is wacky.
* Mobile support (put chat window on another screen)
* Show whose turn it is.
* Overall refactor the design code.
* "Start" and "Stop" buttons are unintuitive
  (perhaps move them to center screen with more descriptive names or tooltips?)
* "Others" confusing; maybe "Spectators" instead?
* "Share" button.
* Add non-square icons.

### Server-side
* Change to an FSM model for the Game Room transition logic.
* Store player username info as a cookie.
* Send structured data instead of pure String messages.
* Seperate out the game-logic and the chat-logic of the game room.
