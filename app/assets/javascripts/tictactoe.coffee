$(document).ready ->
  board = $("#board")
  $(window).resize ->
    board.reset()
  socket = new WebSocket(wsURL)

  # Send a turn through the socket
  sendTurn = (row, col) ->
    text = JSON.stringify {kind: 'turn', row, col}
    socket.send text

  for i in [0...3]
    for j in [0...3]
      do (i, j) -> $("##{i}#{j}").click ->
        console.log i, j
        sendTurn i, j

  # Draw the board from the specified board state
  renderBoard = (board) ->
    for i in [0...3]
      for j in [0...3]
        $("##{i}#{j}").text(board[i][j])

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

  board.resize = (newSize) ->
    board.width newSize
    board.height newSize
    $('.tictac').each ->
      $(this).width newSize/3-10
      $(this).height newSize/3-10

  board.selectTile = (player,row,column) ->
    tile = $("##{row}#{column}")
    style = ""
    switch player
      when 0 then style = "btn-success"
      when 1 then style = "btn-primary"
    tile.attr("class", tile.attr("class") + " " + style)

  board.reset = () ->
    maxWidth = $('#ttcontainer').width()
    maxHeight = $(window).height() - ($('.navbar').height() + 5)
    height = Math.min(maxHeight, maxWidth)
    board.resize height
    board.center()

  $('.collapse').click ->
    board.reset()

  board.reset()

jQuery.fn.center = () ->
  this.css("position","absolute")
  this.css("top", ( $(window).height() - this.height() + ($('.navbar').height() + 5) ) / 2+$(window).scrollTop() + "px")
  this.css("left", ( $(window).width() - this.width() ) / 2+$(window).scrollLeft() + "px")
  return this
