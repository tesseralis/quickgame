$(document).ready ->
  board = new Board 3,3, (i, j) ->
    sendTurn i, j

  board.eachTile (tile) ->
    tile.addClass("btn")
    tile.addClass("ttt-tile")
  
  $('#ttcontainer').append(board.toHTML())

  $(window).resize ->
    reset()

  socket = new WebSocket(wsURL)

  # Send a turn through the socket
  sendTurn = (row, col) ->
    text = JSON.stringify {kind: 'move', row, col}
    socket.send text

  # Draw the board from the specified board state
  renderBoard = (jsonBoard) ->
    board.eachTile (tile,i,j) ->
      style = switch jsonBoard[i][j]
        when 0 then "btn-success"
        when 1 then "btn-primary"
        else ""
      tile.addClass(style)
  # Request the state so we have it initially
  socket.onopen = (evt) ->
    socket.send JSON.stringify {kind: 'request'}

  # Socket message callback
  socket.onmessage = (msg) ->
    data = $.parseJSON msg.data
    renderBoard data.state.board
    switch data.kind
      when 'turn'
        $('#message').text "It is #{data.state.player}'s turn"
      when 'win'
        $('#message').text "#{data.state.player} is the winner!"
      when 'draw'
        $('#message').text "It's a draw!"

  reset = () ->
    top = $('#ttcontainer').position().top
    height = $(window).height() - (top + 10)
    $('#ttcontainer').height(height)
    board.reset()
  
  reset()

