
append = (message) ->
	$("#chattext").append("<p>"+message+"</p>")

websocketurl = (url) ->
	(url.replace /http/, "ws") + "/stream?username="+username

messageType = {}
messageType["join"] = (joinMap) ->
	append joinMap.user + " " +joinMap.message
	console.log joinMap
messageType["talk"] = (talkMap) ->
	append talkMap.user + " : " +talkMap.message
	console.log talkMap
messageType['default'] = (defaultMap) ->
	append "Does not know what to do with "+defaultMap.kind
	console.log defaultMap

receiveMessage = (json) ->
	jsonObject = $.parseJSON(json)
	kind = String(jsonObject.kind)
	if messageType[kind]
		messageType[kind](jsonObject)
	else
		messageType['default'](jsonObject)

$(document).ready ->
	socket = new WebSocket(websocketurl document.URL, 'soap')
	socket.onmessage = (e) ->
		receiveMessage e.data
	socket.onopen = () ->
		#
	$('#submitButton').bind 'click', (event) =>
		dialog = $("#messageText")
		text = JSON.stringify({text: dialog.val()})
		socket.send(text)
		dialog.val("")