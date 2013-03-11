# An extension of the websocket interface that deals with sending game data.
class GameSocket
  _funs = {}
  _super = undefined
  constructor: (wsURL, onOpen) ->
    _super = new WebSocket(wsURL)
    _super.onopen = (evnt) ->
      onOpen evnt
    _funs = {}
    _super.onmessage = (msg) =>
      data = $.parseJSON msg.data
      if fn = _funs[data.kind]
        fn data.data
      else
        console.warn "Socket events for '#{ data.kind }' has not been defined."

  send: (kind, data) ->
    _super.send JSON.stringify {kind, data}

  bind: (name, fn) =>
    _funs[name] = fn

$(document).ready ->
  window.socket = new GameSocket wsURL, ->
    socket.send 'update', ['members', 'players', 'gamestate']
