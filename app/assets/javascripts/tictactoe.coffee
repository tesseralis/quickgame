renderBoard = (board) ->
  for i in [0...3]
    for j in [0...3]
      $("#board .#{i} .#{j}")
        .text(board[i][j])
        .click -> unless board[i][j] in 'xo' then sendTurn i, j

sendTurn = (i, j) ->
  text = JSON.stringify {type: 'turn', i, j}
  socket.send text

$(document).ready ->
  socket = new WebSocket(wsURL)
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

