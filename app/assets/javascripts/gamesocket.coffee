# An extension of the websocket interface that deals with sending game data.
# TODO Figure out how to extend WebSocket instead of composition
class GameSocket
  _funs = {} # Callback functions for different messages
  _super = undefined # The underlying WebSocket

  constructor: (wsURL) ->
    # Initialize the private variables
    _super = new WebSocket(wsURL)
    _funs = {}

    # Route the received messages to our callback functions
    _super.onmessage = (msg) ->
      {kind, data} = $.parseJSON msg.data
      if (f = _funs[kind])?
        f(data)
      else
        console.warn "Received undefined event #{kind}."

  send: (kind, data) ->
    _super.send JSON.stringify {kind, data}

  # TODO Multiple bindings
  bind: (name, f) ->
    _funs[name] = f

  onopen: (f) -> _super.onopen = f

# TODO Should we include this in a different file?
$(document).ready ->
  window.socket = new GameSocket wsURL
  socket.onopen -> socket.send 'update', ['members', 'players', 'gamestate']
