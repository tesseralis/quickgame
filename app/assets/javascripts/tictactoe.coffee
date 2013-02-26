$(document).ready ->
  socket = new WebSocket(wsURL)

  # Send a turn through the socket
  sendTurn = (i, j) ->
    text = JSON.stringify {type: 'turn', i, j}
    socket.send text

  # Draw the board from the specified board state
  renderBoard = (board) ->
    for i in [0...3]
      for j in [0...3]
        $("#board .#{i} .#{j}")
          .text(board[i][j])
          .click -> unless board[i][j] in 'xo' then sendTurn i, j

  # Socket message callback
  socket.onmessage = (msg) ->
    data = $.parseJSON msg
    renderBoard data.board
    switch data.type
      when 'turn'
        $('#message').text "It is #{data.player}'s turn"
      when 'win'
        $('#message').text "The winner is #{data.player}"
      when 'draw'
        $('#message').text "The game is a draw!"

