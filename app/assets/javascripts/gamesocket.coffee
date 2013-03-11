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
      {kind, data} = $.parseJSON msg.data
      if (fun = _funs[kind])?
        fun(data)
      else
        console.warn "Received undefined event #{kind}."

  send: (kind, data) ->
    _super.send JSON.stringify {kind, data}

  bind: (name, fn) =>
    _funs[name] = fn

$(document).ready ->
  window.socket = new GameSocket wsURL, ->
    socket.send 'update', ['members', 'players', 'gamestate']
