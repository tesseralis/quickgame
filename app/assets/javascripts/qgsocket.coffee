# An extension of the websocket interface that deals with sending game data.
class GameSocket
  constructor: (wsURL, onOpen) ->
    @socket = new WebSocket(wsURL)
    @socket.onopen = (evnt) ->
      onOpen evnt
    @fns = {}
    @socket.onmessage = (msg) =>
      data = $.parseJSON msg.data
      if fn = @fns[data.kind]
        fn data.data
      else
        console.warn "Socket events for '#{ data.kind }' has not been defined."

  send: (kind, data) ->
    @socket.send JSON.stringify {kind, data}

  bind: (name, fn) =>
    @fns[name] = fn

$(document).ready ->
  window.socket = new GameSocket wsURL, ->
    socket.send 'update', ['members', 'players', 'gamestate']
