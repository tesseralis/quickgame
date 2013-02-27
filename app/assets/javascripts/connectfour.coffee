$(document).ready ->
  board = new Board 6, 7, (i, j) ->
    sendTurn i, j

  board.eachTile (tile) ->
    tile.addClass("btn")
    tile.addClass("ttt-tile")
  
  $('#cfcontainer').append(board.toHTML())

  $(window).resize ->
    reset()

  socket = new WebSocket(wsURL)

  # Send a turn through the socket
  sendTurn = (row, col) ->
    text = JSON.stringify {kind: 'turn', row, col}
    socket.send text

  # Draw the board from the specified board state
  renderBoard = (jsonBoard) ->
    board.eachTile (tile,i,j) ->
      style = switch jsonBoard[i][j]
        when 0 then "btn-success"
        when 1 then "btn-primary"
        else ""
      tile.addClass(style)
  # Socket message callback
  socket.onmessage = (msg) ->
    data = $.parseJSON msg.data
    renderBoard data.board
    switch data.kind
      when 'turn'
        $('#message').text "It is #{data.player}'s turn"
      when 'win'
        $('#message').text "#{data.player} is the winner!"
      when 'draw'
        $('#message').text "It's a draw!"

  reset = () ->
    top = $('#cfcontainer').position().top
    height = $(window).height() - (top + 10)
    $('#cfcontainer').height(height)
    board.reset()
  
  reset()

