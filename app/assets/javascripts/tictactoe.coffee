$(document).ready ->
  newBoard = new Board 3,3, (i, j) ->
    sendTurn i, j

  newBoard.eachTile (tile) ->
    tile.addClass("btn")
    tile.css({
      "border-style":"solid",
      "border-width":"1px",
      "border-color":"#FFFFFF"
    })
  
  $('#ttcontainer').append(newBoard.toHTML())

  board = $("#board")
  $(window).resize ->
    reset()
    board.reset()
  socket = new WebSocket(wsURL)

  # Send a turn through the socket
  sendTurn = (row, col) ->
    text = JSON.stringify {kind: 'turn', row, col}
    socket.send text


  # Draw the board from the specified board state
  renderBoard = (board) ->
    for i in [0...3]
      for j in [0...3]
        $("##{i}#{j}").text(board[i][j])

  # Socket message callback
  socket.onmessage = (msg) ->
    data = $.parseJSON msg.data
    switch data.kind
      when 'state'
        $('#message').text "It is #{data.player}'s turn"
        renderBoard data.board
      when 'status'
        $('#message').text data.text

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
    maxWidth = $('.container').width()
    maxHeight = $(window).height() - ($('.navbar').height() + 5)
    height = Math.min(maxHeight, maxWidth)
    board.resize height
    board.center()

  board.center = () ->
    this.css("position","absolute");
    this.css("top", ( $(window).height() - this.height() + ($('.navbar').height() + 5) ) / 2+$(window).scrollTop() + "px");
    this.css("left", ( $(window).width() - this.width() ) / 2+$(window).scrollLeft() + "px");
    return this;

  reset = () ->
    top = $('#ttcontainer').position().top
    height = $(window).height() - (top + 10)
    $('#ttcontainer').height(height)
    newBoard.reset()
  
  board.reset()
  reset()

