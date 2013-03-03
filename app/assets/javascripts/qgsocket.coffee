class window.QGSocket
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

	send: (dataMap) =>
		@socket.send JSON.stringify dataMap

	bind: (name, fn) =>
		@fns[name] = fn

$(document).ready ->
	window.socket = new QGSocket(wsURL, () ->
		socket.send {kind: 'update', data: ['members', 'players', 'gamestate']}
		)