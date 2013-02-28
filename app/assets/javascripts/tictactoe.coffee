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
    text = JSON.stringify {kind: 'move', data: {row, col}}
    socket.send text

  # Draw the board from the specified board state
  renderBoard = (jsonBoard) ->
    board.eachTile (tile, i, j) ->
      style = switch jsonBoard[i][j]
        when 0 then "btn-success"
        when 1 then "btn-primary"
        else ""
      tile.addClass(style)
  # Request the state so we have it initially
  socket.onopen = (evt) ->
    socket.send JSON.stringify {kind: 'update', data: ['members', 'players', 'gamestate']}

  # Socket message callback
  socket.onmessage = (msg) ->
    data = $.parseJSON msg.data
    switch data.kind
      when 'gamestate' then renderBoard data.data.board

  reset = () ->
    top = $('#ttcontainer').position().top
    height = $(window).height() - (top + 10)
    $('#ttcontainer').height(height)
    board.reset()
  
  reset()

