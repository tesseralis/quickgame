
append = (message) ->
	chattext = $("#chattext")
	newline = $('<p>').text(message)
	chattext.append(newline)
	chattext.scrollTop(chattext.height())
	$('html, body').scrollTop($(document).height()-$(window).height());

messageType = {}
messageType["join"] = (joinMap) ->
	append joinMap.user + " " +joinMap.message
messageType["talk"] = (talkMap) ->
	append talkMap.user + " : " +talkMap.message
messageType['default'] = (defaultMap) ->
	append defaultMap.user + " " +defaultMap.message

receiveMessage = (json) ->
	jsonObject = $.parseJSON(json)
	kind = String(jsonObject.kind)
	if messageType[kind]
		messageType[kind](jsonObject)
	else
		messageType['default'](jsonObject)

$(document).ready ->
	socket = new WebSocket(wsURL)
	socket.onmessage = (e) ->
		receiveMessage e.data
	socket.onopen = () ->
		append "Welcome to the chat room!"
	$('#submitButton').bind 'click', (event) =>
		sendMessage()
	$('#messageText').bind 'keypress', (e) =>
		if e.which == 13
        	sendMessage()

	sendMessage = () ->
		dialog = $("#messageText")
		text = JSON.stringify({text: dialog.val()})
		socket.send(text)
		dialog.val("")

