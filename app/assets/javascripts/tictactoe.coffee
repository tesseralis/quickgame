$(document).ready ->
  board = new Board 3,3, (i, j) ->
    sendTurn i, j

  board.eachTile (tile) ->
    tile.addClass("btn")
    tile.addClass("ttt-tile")
  
  $('#ttcontainer').append(board.toHTML())

  $(window).resize ->
    reset()
  
  socket.bind "gamestate", (data) ->
    renderBoard data.board


  # Send a turn through the socket
  sendTurn = (row, col) ->
    #socket.send {kind: 'move', data: {row, col}}
    socket.send 'move', {row, col}

  # Draw the board from the specified board state
  renderBoard = (jsonBoard) ->
    board.eachTile (tile, i, j) ->
      style = switch jsonBoard[i][j]
        when 0 then "btn-success"
        when 1 then "btn-primary"
        else ""
      tile.removeClass("btn-success btn-primary").addClass(style)


  reset = () ->
    top = $('#ttcontainer').position().top
    height = $(window).height() - (top + 10)
    $('#ttcontainer').height(height)
    board.reset()

  window.qgsocketmade = (abc) ->
    alert(abc)

  reset()

