$(document).ready ->
  messageInput = $ '#messageInput'
  display = $ '#messagePanel'
  displayText = $ '#messageText'

  playersDiv = $ '#players'
  othersDiv = $ '#others'

  nameInput = $ '#nameInput'

  $(window).resize ->
    resize()

  $('#start').click ->
    socket.send kind: 'start'

  $('#stop').click ->
    socket.send kind: 'stop'

  socket.bind 'members', ({players, others}) ->
    playersDiv.html ''
    for player, i in players then do (i) ->
      li = $ '<li>'
      li.text player
      li.click ->
        socket.send kind: 'changerole', data: i
      playersDiv.append li

    othersDiv.text others.join ', '

  socket.bind "message", (data) ->
    displayText.append($("<p>").text data)
    display.scrollTop(displayText.height())

  resize = () ->
    parent = $("#messageInput").parent()
    messageInput.width(parent.width())
    display.width(parent.width())
    display.height(messageInput.position().top-parent.position().top)

  messageInput.bind 'keypress', (e) ->
    if e.which == 13
      message = messageInput.val()
      if message.length > 0
        socket.send {kind: "chat", data: message}
        messageInput.val("")

  nameInput.bind 'keypress', (e) ->
    if e.which == 13
      message = nameInput.val()
      if message.length > 0
        socket.send kind: 'changename', data: message

  resize()
  messageInput.focus()
