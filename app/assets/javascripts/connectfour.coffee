$(document).ready ->
  board = new Board 6, 7, (i, j) ->
    sendTurn j

  board.eachTile (tile) ->
    tile.addClass("btn")
    tile.addClass("ttt-tile")
  
  $('#cfcontainer').append(board.toHTML())

  $(window).resize ->
    reset()

  socket = new WebSocket(wsURL)

  # Send a turn through the socket
  sendTurn = (col) ->
    text = JSON.stringify {kind: 'move', data: col}
    socket.send text

  # Draw the board from the specified board state
  renderBoard = (jsonBoard) ->
    board.eachTile (tile, i, j) ->
      style = switch jsonBoard[j][5-i]
        when 0 then "btn-success"
        when 1 then "btn-primary"
        else ""
      tile.addClass(style)
  socket.onopen = (evt) ->
    socket.send JSON.stringify {kind: 'update', data: ['members', 'players', 'gamestate']}
  # Socket message callback
  socket.onmessage = (msg) ->
    {kind, data} = $.parseJSON msg.data
    switch kind
      when 'gamestate' then renderBoard data.board

  reset = () ->
    top = $('#cfcontainer').position().top
    height = $(window).height() - (top + 10)
    $('#cfcontainer').height(height)
    board.reset()
  
  reset()

