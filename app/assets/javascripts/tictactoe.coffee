$(document).ready ->
  board = $("#board")
  $(window).resize ->
    # board.resize($("#ttcontainer").width())
    board.center()
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
      when 1 then style = "btn-danger"
    tile.attr("class", tile.attr("class") + " " + style)


  board.resize(Math.min($('body').height(),$('body').width()))
  board.center()

jQuery.fn.center = () ->
    this.css("position","absolute");
    this.css("top", ( $(window).height() - this.height() ) / 2+$(window).scrollTop() + "px");
    this.css("left", ( $(window).width() - this.width() ) / 2+$(window).scrollLeft() + "px");
    return this;