append = (message) ->
	$("#chattext").append("<p>"+message+"</p>")

websocketurl = (url) ->
	(url.replace /http/, "ws") + "/stream"

$(document).ready ->
	socket = new WebSocket(websocketurl document.URL)
	socket.onmessage = (e) ->
		append e.data
	$('#submitButton').bind 'click', (event) =>
		socket.send(("#messageText").text())