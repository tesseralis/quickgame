$(document).ready ->
  input = $ '#messageInput'
  display = $ '#messagePanel'
  displayText = $ '#messageText'

  playersDiv = $ '#players'
  othersDiv = $ '#others'

  $(window).resize ->
    resize()

  socket.bind 'members', ({players, others}) ->
    playersDiv.html ''
    for player, i in players
      li = $ '<li>'
      li.text player
      li.click -> socket.send kind: 'changerole', data: i
      playersDiv.append li

    othersDiv.html ''
    for other in others
      othersDiv.append($("<li>").text other)

  socket.bind "message", (data) ->
    displayText.append($("<p>").text data)
    display.scrollTop(displayText.height())

  resize = () ->
    parent = $("#messageInput").parent()
    input.width(parent.width())
    display.width(parent.width())
    display.height(input.position().top-parent.position().top)

  input.bind 'keypress', (e) ->
    if e.which == 13
      message = input.val()
      if message.length > 0
        socket.send {kind: "chat", data: message}
        input.val("")

  resize()
  input.focus()
