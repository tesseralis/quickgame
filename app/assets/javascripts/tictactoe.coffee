$(document).ready ->
  board = new Board 3, 3, (row, col) ->
    socket.send 'move', {row, col}

  board.eachTile (tile) ->
    tile.addClass "btn"
    tile.addClass "ttt-tile"
  
  $('#ttcontainer').append board.toHTML()


  $(window).resize ->
    reset()
  
  socket.bind "gamestate", (data) ->
    renderBoard data.board
    $('#player').text "It is Player #{data.player+1}'s turn."

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

  reset()

