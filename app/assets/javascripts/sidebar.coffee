$(document).ready ->
  messageInput = $ '#messageInput'
  display = $ '#messagePanel'
  displayText = $ '#messageText'

  playersDiv = $ '#players'
  spectatorsDiv = $ '#spectators'

  $(window).resize ->
    resize()

  $('#start').click ->
    socket.send 'start'

  $('#stop').click ->
    socket.send 'stop'

  $('#changename').click ->
    name = $('#nameInput').val()
    # Change the name in the room
    socket.send 'changename', name
    # Change the cookie
    $.ajax cookieURL.replace('_', name)

  socket.bind 'members', ({players, others}) ->
    playersDiv.html ''
    for player, i in players then do (i) ->
      li = $ '<li>'
      li.text player
      li.click ->
        socket.send 'changerole', i
      playersDiv.append li

    spectatorsDiv.text others.join ', '

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
        socket.send 'chat', message
        messageInput.val("")

  resize()
  messageInput.focus()
