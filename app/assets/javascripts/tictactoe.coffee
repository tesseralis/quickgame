$(document).ready ->
  board = new Board 3,3, (i, j) ->
    sendTurn i, j

  board.eachTile (tile) ->
    tile.addClass("btn")
    tile.addClass("ttt-tile")
  
  $('#ttcontainer').append(board.toHTML())

  $(window).resize ->
    reset()

  socket = new QGSocket(wsURL, () ->
    socket.send {kind: 'update', data: ['members', 'players', 'gamestate']}
    )

  # Send a turn through the socket
  sendTurn = (row, col) ->
    socket.send {kind: 'move', data: {row, col}}

  # Draw the board from the specified board state
  renderBoard = (jsonBoard) ->
    board.eachTile (tile, i, j) ->
      style = switch jsonBoard[i][j]
        when 0 then "btn-success"
        when 1 then "btn-primary"
        else ""
      tile.addClass(style)

  # Socket message callback
  window.gamestate = (data) ->
    renderBoard data.board

  reset = () ->
    top = $('#ttcontainer').position().top
    height = $(window).height() - (top + 10)
    $('#ttcontainer').height(height)
    board.reset()

  window.qgsocketmade = (abc) ->
    alert(abc)

  reset()

