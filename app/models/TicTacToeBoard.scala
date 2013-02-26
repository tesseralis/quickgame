package models.boards

class TicTacToeBoard(goesFirst: Int = 1){
	require(goesFirst == 1 || goesFirst == 2, "Either player 1 or 2 has to go first")
	private val board = Array.ofDim[Int](3,3)
	private var _nextPlayer = goesFirst
	def nextPlayer = _nextPlayer

	def place(player: Int, row: Int, col: Int) = {
		if(player != nextPlayer){
			(false, "It is player "+nextPlayer+"'s turn.")
		} else {
			_nextPlayer = (player % 2) + 1
		}
		if(board(row)(col) != 0) {
			(false, "Player "+board(row)(col)+" has already gone there")
		} else {
			board(row)(col) = player
		}
	}
}