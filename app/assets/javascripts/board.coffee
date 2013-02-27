class window.Board
	constructor: (@rows, @columns, @click) ->
		@board = $('<div>',{class: "board-frame"})
		@tiles =
			for i in [0...rows]
				for j in [0...columns]
					$('<button>',{class: "board-tile"})
		@eachTile (tile, i, j) => 
			@board.append(tile)
			tile.bind 'click', (event) =>
				@click i, j
		@tileLength = 0
		
	eachTile: (fn) =>
		for i in [0...@rows]
			for j in [0...@columns]
				fn @tiles[i][j], i, j

	get: (row, column) =>
		@tiles[row][column]

	resize: () =>
		parent = @board.parent()
		maxTileWidth = parent.width() / @columns
		maxTileHeight = parent.height() / @rows
		@tileLength = Math.min(maxTileWidth, maxTileHeight)
		@eachTile (tile, i, j) => 
			tile.css({
				"position": "absolute",
				"width": @tileLength,
				"height": @tileLength,
				"top": i * @tileLength,
				"left": j * @tileLength})

	center: () =>
		parent = @board.parent()
		width = @tileLength * @columns
		height = @tileLength * @rows
		@board.css({
			"position": "relative",
			"width": width,
			"height": height,
			"top": 0,
			"margin-left": "auto",
			"margin-right":"auto"})
	
	reset: () =>
		@resize()
		@center()

	toHTML: =>
		@board
			  