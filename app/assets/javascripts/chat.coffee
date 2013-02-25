
append = (message, icon = "") ->
	chattext = $("#chattext")
	newline = $('<p>').text(message)
	iconDiv = $('<i>')
	iconDiv.attr("class",icon)
	newline.prepend(iconDiv)
	chattext.append(newline)
	chattext.scrollTop(chattext.height())
	$('html, body').scrollTop($(document).height()-$(window).height());

messageType = {}
messageType["join"] = (joinMap) ->
	append(joinMap.user + " " +joinMap.message, "icon-user")
messageType["quit"] = (joinMap) ->
	append(joinMap.user + " " +joinMap.message, "icon-remove")
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
		message = dialog.val()
		if message.length > 0
			text = JSON.stringify({text: message})
			socket.send(text)
			dialog.val("")
	$("#messageText").focus()

