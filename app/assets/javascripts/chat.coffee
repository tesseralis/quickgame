
append = (message, icon = "") ->
  chattext = $("#chattext")
  newline = $('<p>').text(message)
  iconDiv = $('<i>')
  iconDiv.attr("class",icon)
  newline.prepend(iconDiv)
  chattext.append(newline)
  chattext.scrollTop(chattext.height())
  $('html, body').scrollTop($(document).height()-$(window).height())

messageType = {}
messageType["join"] = (joinMap) ->
  append("#{joinMap.user} has joined the room.", "icon-user")
messageType["quit"] = (joinMap) ->
  append("#{joinMap.user} has left the room.", "icon-remove")
messageType["move"] = (talkMap) ->
  append talkMap.user + " : " +talkMap.state
messageType['default'] = (defaultMap) ->
  append defaultMap.user + " " +defaultMap.state

receiveMessage = (json) ->
  jsonObject = $.parseJSON(json)
  kind = String(jsonObject.kind)
  if messageType[kind]
    messageType[kind](jsonObject)
  else
    messageType['default'](jsonObject)

changePort = (url,newPort) ->
  tag = document.createElement('a')
  tag.href = url
  tag.port = newPort
  return tag.href

$(document).ready ->
  socket = new WebSocket(wsURL)
  # socket = new WebSocket(changePort(wsURL,9000))
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
      text = JSON.stringify({kind: "move", text: message})
      socket.send(text)
      dialog.val("")
  $("#messageText").focus()

