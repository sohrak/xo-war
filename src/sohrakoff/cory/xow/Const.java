package sohrakoff.cory.xow;

/**
 * 
 * This interface contains constant values used by the application. Each value's
 * usage is explained in the comment preceeding the value.
 * 
 * @author cory
 *
 */
public interface Const {
	
	// constants related to the size of the game board
	public static final int GAME_BOARD_SIZE = 3;
	public static final int GAME_BOARD_LINES = GAME_BOARD_SIZE - 1;
	public static final int NUM_GAME_BOARD_SQUARES = GAME_BOARD_SIZE*GAME_BOARD_SIZE;
	public static final int SQUARE_SPACING = 5; // in pixels
	public static final int LINE_THICKNESS = SQUARE_SPACING;

	// max number of pieces a player can have out
	public static final int MAX_NUMER_PIECES = 3;
	
	// Menu Constant
	public static final int MENU_NEW_GAME = 0;
	public static final int MENU_ABOUT = 1;
	
	// Constant array for new game list view
	public static final String[] newGameList = { "1 Player Game", "2 Player Game" };
	
	// positions in the above string array
	public static final int ONE_PLAYER = 0;
	public static final int TWO_PLAYERS = 1;
	
	// game state constants
	public static final int PLAYER_1_INPUT = 1;
	public static final int PLAYER_2_INPUT = 2;
	public static final int PLAYER_1_DRAW = 3;
	public static final int PLAYER_2_DRAW = 4;
	public static final int PLAYER_1_CHECK = 5;
	public static final int PLAYER_2_CHECK = 6;
	public static final int GAME_OVER = 0;
	
	// winner constants
	public static final int PLAYER_1 = 1;
	public static final int PLAYER_2 = 2;
	public static final int NO_WINNER = 0;
	
	// winning combinations
	public static final int WINNING_COMBINATION_LENGTH = 3;
	public static final int NUM_WINNING_COMBINATIONS = 8;
	public static final int[][] WINNING_COMBINATIONS = { {0,1,2}, {3,4,5}, {6,7,8}, 
		{0,3,6}, {1,4,7}, {2,5,8}, {0,4,8}, {2,4,6} };
	
	
	// possible move constants
	public static final int WIN_NOT_POSSIBLE = -1;
	public static final int BLOCK_NOT_POSSIBLE = -1;
	public static final int SETUP_NOT_POSSIBLE = -1;
	
	// piece locations
	public static final int CENTER = 4;
	public static final int[] CORNERS = {0,2,6,8};
	public static final int[] EDGES = {1,3,5,7};
	public static final int NOWHERE = -1;
	
	// animation delays
	public static final long CPU_THINKING_TIME = 1100;
	public static final long GAME_WIN_DELAY = 500; // delay until game winner is announced
}
