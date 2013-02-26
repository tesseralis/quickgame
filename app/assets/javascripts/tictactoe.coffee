$(document).ready ->
  socket = new WebSocket(wsURL)

  # Send a turn through the socket
  sendTurn = (row, col) ->
    text = JSON.stringify {kind: 'turn', row, col}
    socket.send text

  for i in [0...3]
    for j in [0...3]
      do (i, j) -> $("##{i}#{j}").click ->
        console.log i, j
        sendTurn i, j

  # Draw the board from the specified board state
  renderBoard = (board) ->
    for i in [0...3]
      for j in [0...3]
        $("##{i}#{j}").text(board[i][j])

  # Socket message callback
  socket.onmessage = (msg) ->
    data = $.parseJSON msg.data
    switch data.kind
      when 'state'
        $('#message').text "It is #{data.player}'s turn"
        renderBoard data.board
      when 'status'
        $('#message').text data.text

