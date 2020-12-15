package sohrakoff.cory.xow;

import android.R.color;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

public class GameThread extends Thread {

	// tag for use with log messages
	private static final String TAG = "GameThread";
	
	// Thread state data
	private Context context;
	private SurfaceHolder surfaceHolder;
	private Handler messageHandler;
	private boolean running;
	
	// Paint color
	private Paint paint;
	private Paint winPaint;
	
	// Variables needed to draw and set up the view
	private int horizontalPadding;
	private int verticalPadding;
	//private int lineLength;
	private Rect[] squareAreas;
	//private int[] horizontalLines;
	//private int[] verticalLines;
	
	// Game state data
	private int gameType;
	private int gameState;
	private int winner;
	private int[] winningComb;
	
	// Player data
	private int[] playerOnePieces;
	private int playerOneNumPieces;
	private int[] playerTwoPieces;
	private int playerTwoNumPieces;
	
	// used for CPU player movement
	private boolean firstTurn;
	
	// drawables for game graphics
	Drawable xPiece = null;
	Drawable oPiece = null;
	Drawable square = null;

	public GameThread( Context context, SurfaceHolder surfaceHolder, Handler messageHandler ) {
		this.context = context;
		this.surfaceHolder = surfaceHolder;
		this.messageHandler = messageHandler;
		
		// Get Drawables
		square = context.getResources().getDrawable(R.drawable.square);
		oPiece = context.getResources().getDrawable(R.drawable.o);
		xPiece = context.getResources().getDrawable(R.drawable.x);
		/*
		// Set up line paint color
		paint = new Paint();
		paint.setColor(Color.LTGRAY);
		paint.setStrokeWidth(Const.LINE_THICKNESS);
		paint.setTextSize(24);
		
		// Set up win paint color
		winPaint = new Paint();
		winPaint.setColor(Color.YELLOW);
		winPaint.setStrokeWidth(Const.LINE_THICKNESS);
		winPaint.setTextSize(24); */
	}
	
	@Override
	public void run() {
		while (running) {
			Canvas canvas = null;
			try {
				canvas = surfaceHolder.lockCanvas();
				synchronized (surfaceHolder) {
					doUpdate();
					doDraw(canvas);
				}
			} finally {
				if ( canvas != null )
					surfaceHolder.unlockCanvasAndPost(canvas);
			}
		}
	}
	
	public void setRunning(boolean running) {
			this.running = running;	
	}
	
	// update game state and run CPU player input from here
	private void doUpdate() {
		synchronized (surfaceHolder) {
			if (gameState == Const.PLAYER_1_DRAW)
				gameState = Const.PLAYER_1_CHECK;
			else if (gameState == Const.PLAYER_2_DRAW)
				gameState = Const.PLAYER_2_CHECK;
			else if (gameState == Const.PLAYER_1_CHECK) {
				// check for win and adjust state accordingly
				if(checkWinner(playerOneNumPieces, playerOnePieces)) {
					Log.v(TAG, "P1 WINNER");
					winner = Const.PLAYER_1;
					Message msg = new Message();
					Bundle data = new Bundle();
					data.putInt("titleId", R.string.player_1_win);
					msg.setData(data);
					messageHandler.sendMessage(msg);
					gameState = Const.GAME_OVER;
				}
				else {
					Message msg = new Message();
					Bundle data = new Bundle();
					if ( gameType == Const.TWO_PLAYERS )
						data.putInt("titleId", R.string.player_2_turn);
					else
						data.putInt("titleId", R.string.cpu_turn);
					msg.setData(data);
					messageHandler.sendMessage(msg);
					gameState = Const.PLAYER_2_INPUT;
				}
			}
			else if (gameState == Const.PLAYER_2_CHECK) {
				if(checkWinner(playerTwoNumPieces, playerTwoPieces)) {
					Log.v(TAG, "P2 WINNER");
					winner = Const.PLAYER_2;
					Message msg = new Message();
					Bundle data = new Bundle();
					if ( gameType == Const.TWO_PLAYERS )
						data.putInt("titleId", R.string.player_2_win);
					else
						data.putInt("titleId", R.string.cpu_win);
					msg.setData(data);
					messageHandler.sendMessage(msg);
					gameState = Const.GAME_OVER;
				}
				else {
					gameState = Const.PLAYER_1_INPUT;
					Message msg = new Message();
					Bundle data = new Bundle();
					data.putInt("titleId", R.string.player_1_turn);
					msg.setData(data);
					messageHandler.sendMessage(msg);
				}
			}
			else if (gameState == Const.PLAYER_2_INPUT && gameType == Const.ONE_PLAYER) {
				CPU_Move();
				gameState = Const.PLAYER_2_DRAW;
				// sleep to simulate CPU player thinking
				try { sleep(Const.CPU_THINKING_TIME); } catch (Exception e) {}
			}
				
		}
	}
	
	private void CPU_Move() {
		synchronized (surfaceHolder) {
			//Log.v(TAG, "CPU_Move()");	
			
			int nextPieceLocation;
			
			if (firstTurn) {
				if (playerOnePieces[0] == Const.CENTER) { // if player 1 places first piece in center
					nextPieceLocation = Const.CORNERS[(int)(Math.random() * (double)Const.CORNERS.length)];
					//Log.v(TAG, "CPU_Move(): corner choice=" + nextPieceLocation);
					addMove(nextPieceLocation);
				}
				else { // player 1 played a piece on an edge or a corner
					   // place piece in center
					addMove(Const.CENTER);
				}					
				firstTurn = false; // no longer first turn
				return; // done so return
			}
			// else not the first turn
			
			int[] openSquares = findOpenSquares();
			
			// try to go for win if possible
			if (playerTwoNumPieces >= (Const.MAX_NUMER_PIECES - 1) 
					&& (nextPieceLocation = tryToWin(openSquares)) != Const.WIN_NOT_POSSIBLE ) 
			{
				//Log.v(TAG, "CPU_Move(): win possible at " + nextPieceLocation);
				addMove(nextPieceLocation);
				return;
			}
			
			// block opponent win
			if (playerOneNumPieces >= (Const.MAX_NUMER_PIECES - 1) 
					&& (nextPieceLocation = tryToBlock(openSquares)) != Const.BLOCK_NOT_POSSIBLE ) 
			{
				//Log.v(TAG, "CPU_Move(): block possible at " + nextPieceLocation);
				addMove(nextPieceLocation);
				return;
			}
			
			// try to set up 2 in a row for next turn
			if (playerTwoNumPieces > 0 // need at least one piece to set up 2 in a row
					&& (nextPieceLocation = tryToSetupWin(openSquares)) != Const.SETUP_NOT_POSSIBLE ) 
			{
				//Log.v(TAG, "CPU_Move(): setup possible at " + nextPieceLocation);
				addMove(nextPieceLocation);
				return;
			}
			
			// if all else fails place a random piece from open squares
			nextPieceLocation = openSquares[(int)(Math.random() * (double)openSquares.length)];
			//Log.v(TAG, "CPU_Move: random square chosen=" + nextPieceLocation);
			addMove(nextPieceLocation);
			return;
		}
	}
	
	private int[] findOpenSquares() {
		synchronized (surfaceHolder) {
			int[] openSquares = new int[Const.NUM_GAME_BOARD_SQUARES-playerOneNumPieces-playerTwoNumPieces];
			int count = 0;
			//Log.v(TAG, "findOpenSquares: length=" + openSquares.length);
			
			
			outerloop:
			for (int i = 0; i < Const.NUM_GAME_BOARD_SQUARES; i++) {
				boolean found = false;
				for (int j = 0; j < playerOneNumPieces; j++) {
						found = found || (playerOnePieces[j] == i);
				}
				if (found)
					continue outerloop;
				
				for (int j = 0; j < playerTwoNumPieces; j++) {
					found = found || (playerTwoPieces[j] == i);
				}
				if (!found)
					openSquares[count++] = i;		
			}
			for (int i = 0; i < openSquares.length; i++)
				; //Log.v(TAG, "Open Square=" + openSquares[i]);
			return openSquares;
		}
	}
	
	/**
	 * CPU Player checks if win is possible this turn
	 * @return WIN_NOT_POSSIBLE or place to put piece to win
	 */
	private int tryToWin(int[] openSquares) {
		synchronized (surfaceHolder) {
			int winLocation = Const.WIN_NOT_POSSIBLE;
			if (playerTwoNumPieces < Const.MAX_NUMER_PIECES)
			{
				// check pieces 0 and 1
				outer_loop:
				for ( int i = 0; i < Const.NUM_WINNING_COMBINATIONS; i++) {
					boolean match = false;
					inner_loop0:
					for ( int j = 0; j < Const.WINNING_COMBINATION_LENGTH; j++ ) {
						if (Const.WINNING_COMBINATIONS[i][j] == playerTwoPieces[0]) {
							match = true;
							break inner_loop0;
						}
					}
					
					if (!match)
						continue outer_loop; // short-circuit if no match
						
					match = false; // reset match flag 
					
					inner_loop1:
					for ( int j = 0; j < Const.WINNING_COMBINATION_LENGTH; j++ ) {
						if (Const.WINNING_COMBINATIONS[i][j] == playerTwoPieces[1]) {
							match = true;
							break inner_loop1;
						}
					}
					
					if (!match)
						continue outer_loop; // short-circuit if no match
					
					for ( int j = 0; j < openSquares.length; j++ ) {
						for ( int k = 0; k < Const.WINNING_COMBINATION_LENGTH; k++ ) {
							if ( openSquares[j] == Const.WINNING_COMBINATIONS[i][k] ) {
								winLocation = openSquares[j];
								//Log.v(TAG, "Found place to win at square=" + openSquares[j]);
								break outer_loop;
							}
						}
					}
				}
			}
			else { // piece 0 is going to expire after move
				// check pieces 1 and 2
				outer_loop:
					for ( int i = 0; i < Const.NUM_WINNING_COMBINATIONS; i++) {
						boolean match = false;
						inner_loop0:
						for ( int j = 0; j < Const.WINNING_COMBINATION_LENGTH; j++ ) {
							if (Const.WINNING_COMBINATIONS[i][j] == playerTwoPieces[1]) {
								match = true;
								break inner_loop0;
							}
						}
						
						if (!match)
							continue outer_loop; // short-circuit if no match
							
						match = false; // reset match flag 
						
						inner_loop1:
						for ( int j = 0; j < Const.WINNING_COMBINATION_LENGTH; j++ ) {
							if (Const.WINNING_COMBINATIONS[i][j] == playerTwoPieces[2]) {
								match = true;
								break inner_loop1;
							}
						}
						
						if (!match)
							continue outer_loop; // short-circuit if no match
						
						for ( int j = 0; j < openSquares.length; j++ ) {
							for ( int k = 0; k < Const.WINNING_COMBINATION_LENGTH; k++ ) {
								if ( openSquares[j] == Const.WINNING_COMBINATIONS[i][k] ) {
									winLocation = openSquares[j];
									//Log.v(TAG, "Found place to win at square=" + openSquares[j]);
									break outer_loop;
								}
							}
						}
					}
			}
			return winLocation;
		}
	}
	
	private int tryToBlock(int[] openSquares) {
		synchronized (surfaceHolder) {
			int blockLocation = Const.BLOCK_NOT_POSSIBLE;
			if ( playerOneNumPieces < Const.MAX_NUMER_PIECES) {
				// check pieces 0 and 1
				outer_loop:
				for ( int i = 0; i < Const.NUM_WINNING_COMBINATIONS; i++) {
					boolean match = false;
					inner_loop0:
					for ( int j = 0; j < Const.WINNING_COMBINATION_LENGTH; j++ ) {
						if (Const.WINNING_COMBINATIONS[i][j] == playerOnePieces[0]) {
							match = true;
							break inner_loop0;
						}
					}
					
					if (!match)
						continue outer_loop; // short-circuit if no match
						
					match = false; // reset match flag 
					
					inner_loop1:
					for ( int j = 0; j < Const.WINNING_COMBINATION_LENGTH; j++ ) {
						if (Const.WINNING_COMBINATIONS[i][j] == playerOnePieces[1]) {
							match = true;
							break inner_loop1;
						}
					}
					
					if (!match)
						continue outer_loop; // short-circuit if no match
					
					for ( int j = 0; j < openSquares.length; j++ ) {
						for ( int k = 0; k < Const.WINNING_COMBINATION_LENGTH; k++ ) {
							if ( openSquares[j] == Const.WINNING_COMBINATIONS[i][k] ) {
								blockLocation = openSquares[j];
								//Log.v(TAG, "Found place to block at square=" + openSquares[j]);
								break outer_loop;
							}
						}
					}
				}
			}
			else {
				// check pieces 1 and 2
				outer_loop:
				for ( int i = 0; i < Const.NUM_WINNING_COMBINATIONS; i++) {
					boolean match = false;
					inner_loop0:
					for ( int j = 0; j < Const.WINNING_COMBINATION_LENGTH; j++ ) {
						if (Const.WINNING_COMBINATIONS[i][j] == playerOnePieces[1]) {
							match = true;
							break inner_loop0;
						}
					}
					
					if (!match)
						continue outer_loop; // short-circuit if no match
						
					match = false; // reset match flag 
					
					inner_loop1:
					for ( int j = 0; j < Const.WINNING_COMBINATION_LENGTH; j++ ) {
						if (Const.WINNING_COMBINATIONS[i][j] == playerOnePieces[2]) {
							match = true;
							break inner_loop1;
						}
					}
					
					if (!match)
						continue outer_loop; // short-circuit if no match
					
					for ( int j = 0; j < openSquares.length; j++ ) {
						for ( int k = 0; k < Const.WINNING_COMBINATION_LENGTH; k++ ) {
							if ( openSquares[j] == Const.WINNING_COMBINATIONS[i][k] ) {
								blockLocation = openSquares[j];
								//Log.v(TAG, "Found place to block at square=" + openSquares[j]);
								break outer_loop;
							}
						}
					}
				}
			}			
			return blockLocation;
		}
	}
	
	private int tryToSetupWin(int[] openSquares) {
		synchronized (surfaceHolder) {
			int setupLocation = Const.SETUP_NOT_POSSIBLE;
			
			//Log.v(TAG, "In tryToSetupWin");
			
			// try to set up force win
			if ( playerOneNumPieces == Const.MAX_NUMER_PIECES ) {
				bigloop:
				for ( int piece = 0; piece < playerTwoNumPieces; piece++ ) {
					if (piece == 0 && playerTwoNumPieces > 1) 
						continue bigloop; // skip checking 0 if it will disappear this turn >2, or next turn >1
					
					if (piece == 1 && playerTwoNumPieces > 2)
						continue bigloop; // skip checking 1 if it will disappear next turn
					
					outer_loop:
					for ( int i = 0; i < Const.NUM_WINNING_COMBINATIONS; i++) {
						boolean match = false;
						inner_loop0:
						for ( int j = 0; j < Const.WINNING_COMBINATION_LENGTH; j++ ) {
							if (Const.WINNING_COMBINATIONS[i][j] == playerTwoPieces[piece]) {
								match = true;
								break inner_loop0;
							}
						}
						
						if (!match)
							continue outer_loop; // short-circuit if no match
							
						match = false; // reset match flag 
						
						inner_loop1:
						for ( int j = 0; j < Const.WINNING_COMBINATION_LENGTH; j++ ) {
							if (Const.WINNING_COMBINATIONS[i][j] == playerOnePieces[0]) {
								match = true;
								break inner_loop1;
							}
						}
							
						if (!match)
							continue outer_loop; // short-circuit if no match
						
						for ( int j = 0; j < openSquares.length; j++ ) {
							for ( int k = 0; k < Const.WINNING_COMBINATION_LENGTH; k++ ) {
								if ( openSquares[j] == Const.WINNING_COMBINATIONS[i][k] ) {
									setupLocation = openSquares[j];
									//Log.v(TAG, "Set up force win at square=" + openSquares[j]);
									break bigloop;
								}
							}
						}
					}
				}
			}
			// TODO
			/* DOESN'T WORK
			if ( setupLocation == Const.SETUP_NOT_POSSIBLE ) { // try to force a block if force win didn't work
				//Log.v(TAG, "Try to force block, npieces=" + playerTwoNumPieces);
				loop0:
				for ( int piece = 0; piece < playerTwoNumPieces; piece++ ) {
					if (piece == 0 && playerTwoNumPieces == Const.MAX_NUMER_PIECES) 
						continue loop0; // skip checking 0 if it will disappear this turn
					
					if (piece == 1 && playerTwoNumPieces >= Const.MAX_NUMER_PIECES - 1)
						continue loop0; // skip checking 1 if it will disappear next turn
					
					loop1:
					for ( int i = 0; i < Const.NUM_WINNING_COMBINATIONS; i++) {
						boolean match = false;
						loop2:
						for ( int j = 0; j < Const.WINNING_COMBINATION_LENGTH; j++ ) {
							if (Const.WINNING_COMBINATIONS[i][j] == playerTwoPieces[piece]) {
								match = true;
								break loop2;
							}
						}
						
						if (!match)
							continue loop1; // short-circuit if no match
						
						for ( int j = 0; j < Const.WINNING_COMBINATION_LENGTH; j++ ) {
							int[] squares = new int[2];
							int count = 0;
							int old = 0;
							for ( int k = 0; k < openSquares.length; k++ ) {
								if ( openSquares[k] == Const.WINNING_COMBINATIONS[i][j] ) {
									if ( count < 2 )
										squares[count++] = openSquares[j];
								}
								
								if ( playerTwoNumPieces == Const.MAX_NUMER_PIECES && 
										playerTwoPieces[0] == Const.WINNING_COMBINATIONS[i][j] ) {
									old++;
								}
							}
							//Log.v(TAG, "count=" + count + ", old=" + old);
							if (count + old >= 2) {
								setupLocation = squares[0];
								//Log.v(TAG, "Can force a block by placing piece at square=" + squares[0] + " or " + squares[1]);
								break loop0;
							}
						}
					}
				}
			}
			*/
			return setupLocation;
		}
	}
	
	private boolean checkWinner(int nPieces, int[] pieces) {
		synchronized (surfaceHolder) {
			if ( nPieces < Const.WINNING_COMBINATION_LENGTH)
				return false;
			
			outerloop:
			for (int i = 0; i < Const.NUM_WINNING_COMBINATIONS; i++ ) {
				for ( int j = 0; j < Const.WINNING_COMBINATION_LENGTH; j++ ) {
					boolean match = false;
					for ( int k = 0; k < Const.WINNING_COMBINATION_LENGTH; k++ ) {
						match = match || (pieces[j] == Const.WINNING_COMBINATIONS[i][k]);
					}
					if (!match)
						continue outerloop;
				}
				winningComb = Const.WINNING_COMBINATIONS[i];
				return true;
			}
			
			return false;
		}
	}
	
	private void doDraw(Canvas canvas) {
		// clear canvas
		canvas.drawColor(color.background_dark);
		
		// quit if anything null
		if (canvas == null || squareAreas == null)
			return;
		/*
		// draw the game board lines
		if (horizontalLines != null && verticalLines != null)
			for ( int i = 0; i < Const.GAME_BOARD_LINES; i++ ) {
				canvas.drawLine(horizontalPadding, horizontalLines[i], horizontalPadding+lineLength, horizontalLines[i], paint);
				canvas.drawLine(verticalLines[i], verticalPadding, verticalLines[i], verticalPadding+lineLength, paint);
			} */
		
		// draw game squares
		for ( int i = 0; i < squareAreas.length; i++) {
			square.setBounds(squareAreas[i]);
			square.draw(canvas);
		}
		
		// draw player 1 pieces
		if ( playerOnePieces != null )
			for (int i = 0; i < playerOneNumPieces; i++ ) {
				xPiece.setBounds(squareAreas[playerOnePieces[i]]);
				xPiece.draw(canvas);
				//canvas.drawText("X", squareAreas[playerOnePieces[i]].exactCenterX(), 
						//squareAreas[playerOnePieces[i]].exactCenterY(), (winner == Const.PLAYER_1) ? winPaint : paint);
			}
		// draw player 2 pieces
		if ( playerTwoPieces != null )
			for (int i = 0; i < playerTwoNumPieces; i++ ) {
				oPiece.setBounds(squareAreas[playerTwoPieces[i]]);
				oPiece.draw(canvas);
				//canvas.drawText("O", squareAreas[playerTwoPieces[i]].exactCenterX(), 
						//squareAreas[playerTwoPieces[i]].exactCenterY(), (winner == Const.PLAYER_2) ? winPaint : paint);
			}
	}
	
	public void setUpScale(int width, int height) {
		synchronized (surfaceHolder) {
			//Log.v(TAG, "setUpScale: Width=" + width + ", Height=" + height);
			
			int squareSize; // determined by smallest (width or height)
			if ( width <= height ) {
				// Const.GAME_BOARD_SIZE+2 because we want at least LIGHT_SPACING padding on each side of screen
				squareSize = (width-(Const.GAME_BOARD_SIZE+2)*Const.SQUARE_SPACING) / Const.GAME_BOARD_SIZE;
			}
			else {
				// Const.GAME_BOARD_SIZE+2 because we want at least LIGHT_SPACING padding on each side of screen
				squareSize = (height-(Const.GAME_BOARD_SIZE+2)*Const.SQUARE_SPACING) / Const.GAME_BOARD_SIZE;
			}
			//Log.v(TAG, "setUpScale: squareSize=" + squareSize);
			
			horizontalPadding = ((width-(Const.GAME_BOARD_SIZE-1)*Const.SQUARE_SPACING) - (squareSize*Const.GAME_BOARD_SIZE)) / 2;
			//Log.v(TAG, "setUpScale: horizontalPadding=" + horizontalPadding);
			verticalPadding = ((height-(Const.GAME_BOARD_SIZE-1)*Const.SQUARE_SPACING) - (squareSize*Const.GAME_BOARD_SIZE)) / 2;
			//Log.v(TAG, "setUpScale: verticalPadding=" + verticalPadding);
			
			// save areas for the game board squares
			squareAreas = new Rect[Const.NUM_GAME_BOARD_SQUARES];
			for ( int row = 0; row < Const.GAME_BOARD_SIZE; row++ )
				for ( int col = 0; col < Const.GAME_BOARD_SIZE; col++ ) {
					int left = horizontalPadding + col*(Const.SQUARE_SPACING + squareSize);
					int top = verticalPadding + row*(Const.SQUARE_SPACING + squareSize);
					squareAreas[row*Const.GAME_BOARD_SIZE+col] = new Rect(left, top, left+squareSize, top+squareSize);
				}
			/*
			// set line length
			if ( width <= height ) {
				lineLength = width - 2*horizontalPadding;
			}
			else {
				lineLength = height - 2*verticalPadding;
			}
			
			// save places to draw the game board lines
			horizontalLines = new int[Const.GAME_BOARD_LINES];
			verticalLines = new int[Const.GAME_BOARD_LINES];
			
			for ( int i = 0; i < Const.GAME_BOARD_LINES; i++ ) {
				horizontalLines[i] = verticalPadding + (i+1)*squareSize + (i)*Const.SQUARE_SPACING;
				verticalLines[i] = horizontalPadding + (i+1)*squareSize + (i)*Const.SQUARE_SPACING;
			} */
	
		}
	}
	
	public void doTouch( int x, int y ) {
		synchronized (surfaceHolder) {			
			// define cases for input to be accepted
			if (gameState == Const.PLAYER_1_INPUT || gameState == Const.PLAYER_2_INPUT)
			{
				// good acceptable state - do nothing
			}
			else {
				return;
			}
			// if CPU player ignore touch input for CPU player
			if (gameState == Const.PLAYER_2_INPUT && gameType == Const.ONE_PLAYER)
				return;
			
			//Log.v(TAG, "Accepted touch");
			boolean squareTouched = false;
			int square;
			
			loop:
			for ( square = 0; square < Const.NUM_GAME_BOARD_SQUARES; square++ )
				if ( squareAreas[square].contains(x, y) ) 
				{
					squareTouched = true;
					//Log.v( TAG, "onTouch: square touched=" + square);
					break loop;
				}
			
			if (squareTouched && legalMove(square)) {
				addMove(square);
			}
		}
	}
	
	private boolean legalMove(int square) {
		synchronized (surfaceHolder) {
			for (int i = 0; i < playerOneNumPieces; i++) {
				if ( square == playerOnePieces[i] )
					return false;
			}
			for (int i = 0; i < playerTwoNumPieces; i++) {
				if ( square == playerTwoPieces[i] )
					return false;
			}
			return true; // if not taken else
		}
	}
	
	private void addMove( int square ) {
		synchronized (surfaceHolder) {			
			if (gameState == Const.PLAYER_1_INPUT) {
				if ( playerOneNumPieces < Const.MAX_NUMER_PIECES ) {
					playerOnePieces[playerOneNumPieces++] = square;
				}
				else { // remove oldest piece
					for ( int i = 0; i < Const.MAX_NUMER_PIECES - 1; i++ )
						playerOnePieces[i] = playerOnePieces[i+1];
					
					playerOnePieces[Const.MAX_NUMER_PIECES - 1] = square;
				}
				gameState = Const.PLAYER_1_DRAW;
			}
			else { // player2 input
				if ( playerTwoNumPieces < Const.MAX_NUMER_PIECES ) {
					playerTwoPieces[playerTwoNumPieces++] = square;
				}
				else { // remove oldest piece
					for ( int i = 0; i < Const.MAX_NUMER_PIECES - 1; i++ )
						playerTwoPieces[i] = playerTwoPieces[i+1];
					
					playerTwoPieces[Const.MAX_NUMER_PIECES - 1] = square;
				}
				gameState = Const.PLAYER_2_DRAW;
			}
		}
	}
	
	public void saveState(Bundle state ) {
		synchronized (surfaceHolder) {
			state.putInt("gameType", gameType);
			state.putInt("gameState", gameState);
			state.putInt("winner", winner);
			state.putIntArray("winningComb", winningComb);
			
			state.putIntArray("playerOnePieces", playerOnePieces);
			state.putInt("playerOneNumPieces", playerOneNumPieces);
			
			state.putIntArray("playerTwoPieces", playerTwoPieces);
			state.putInt("playerTwoNumPieces", playerTwoNumPieces);
			
			state.putBoolean("firstTurn", firstTurn);
		}
	}
	
	public void restoreState(Bundle state) {
		synchronized (surfaceHolder) {
			gameType = state.getInt("gameType");
			gameState = state.getInt("gameState");
			winner = state.getInt("winner");
			winningComb = state.getIntArray("winningComb");
			
			playerOnePieces = state.getIntArray("playerOnePieces");
			playerOneNumPieces = state.getInt("playerOneNumPieces");
			
			playerTwoPieces = state.getIntArray("playerTwoPieces");
			playerTwoNumPieces = state.getInt("playerTwoNumPieces");
			
			firstTurn = state.getBoolean("firstTurn");
		}
	}
	
	public void newGame( int gameType ) {
		synchronized (surfaceHolder) {
			this.gameType = gameType;
			
			playerOneNumPieces = 0;
			playerOnePieces = new int[Const.MAX_NUMER_PIECES];
			
			playerTwoNumPieces = 0;
			playerTwoPieces = new int[Const.MAX_NUMER_PIECES];
			
			gameState = Const.PLAYER_1_INPUT;
			winner = Const.NO_WINNER;
			
			// set for the case of a 1 player game
			firstTurn = true;
			
			Message msg = new Message();
			Bundle data = new Bundle();
			data.putInt("titleId", R.string.player_1_turn);
			msg.setData(data);
			messageHandler.sendMessage(msg);
		}
	}
	
	
}
