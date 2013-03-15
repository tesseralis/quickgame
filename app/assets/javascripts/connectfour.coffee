$(document).ready ->
  board = new Board 6, 7, (_, col) ->
    socket.send 'move', col

  board.eachTile (tile) ->
    tile.addClass "btn"
    tile.addClass "ttt-tile"
  
  $('#cfcontainer').append board.toHTML()

  $(window).resize ->
    reset()

  socket.bind "gamestate", (data) ->
    renderBoard data.board
    $('#player').text switch data.kind
      when 'gamestart' then "It is Player #{data.player+1}'s turn."
      when 'gameend'
        if data.player >= 0
          "Player #{data.player+1} has won!"
        else
          "The game is a draw!"

  # Draw the board from the specified board state
  renderBoard = (jsonBoard) ->
    board.eachTile (tile, i, j) ->
      style = switch jsonBoard[j][5-i]
        when 0 then "btn-success"
        when 1 then "btn-primary"
        else ""
      tile.removeClass("btn-success btn-primary").addClass(style)

  reset = () ->
    top = $('#cfcontainer').position().top
    height = $(window).height() - (top + 10)
    $('#cfcontainer').height(height)
    board.reset()
  
  reset()

