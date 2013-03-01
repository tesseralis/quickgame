class window.QGSocket
	constructor: (wsURL, onOpen) ->
		@socket = new WebSocket(wsURL)
		@socket.onopen = (evnt) ->
			onOpen evnt
		@socket.onmessage = (msg) ->
			data = $.parseJSON msg.data
			fn = window[data.kind]
			if fn
				fn data.data
			else
				console.warn "window.#{ data.kind } is not defined."
			

	send: (dataMap) =>
		@socket.send JSON.stringify dataMap