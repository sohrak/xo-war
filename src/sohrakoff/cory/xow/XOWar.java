package sohrakoff.cory.xow;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * 
 * This is the main activity for the game. It is responsible for setting up the game
 * state and receiving menu input.
 * 
 * @author cory
 *
 */
public class XOWar extends Activity {
	
	// tag for use with log messages
	private static final String TAG = "NoTieTacToe";
	
	private GameView gameView;
	
	private boolean newGameDialogOpen = false;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
 
        gameView = (GameView) findViewById(R.id.gameView); 
        
        if ( savedInstanceState != null ) {
        	setTitle(savedInstanceState.getString("title"));
        	gameView.getGameThread().restoreState(savedInstanceState);
        	if (savedInstanceState.getBoolean("newGameDialogOpen") == true)
        		showNewGameDialog();
        }
        else {
        	showNewGameDialog();
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, Const.MENU_NEW_GAME, 0, R.string.menu_new_game);
			// .setIcon(...
		menu.add(0, Const.MENU_ABOUT, 1, R.string.menu_about);
			// .setIcon(...
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case Const.MENU_NEW_GAME:
				showNewGameDialog();
				return true;
			case Const.MENU_ABOUT:
				showAboutDialog();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		gameView.getGameThread().saveState(outState);
		outState.putString("title", getTitle().toString());
		outState.putBoolean("newGameDialogOpen", newGameDialogOpen);
	}
    
	// Modifies boolean newGameDialogShowing must be saved in OnSIS
	// needs to be public to be called from other places probably
	// if doesnt work can make if private and probably with need to use
	// message/handler
	public void showNewGameDialog() {
		newGameDialogOpen = true;
		
		ListView listView = new ListView(this);
		listView.setAdapter(new ArrayAdapter<String>(this, R.layout.list_row, R.id.word, Const.newGameList));

		
		final AlertDialog ad = new AlertDialog.Builder(this)
			.setCancelable(true)
			.setView(listView)
			.setOnCancelListener( new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					//Log.v(TAG, "NewGameDialog:onCancel: dialog closed");
					newGameDialogOpen = false;
				}
				
			})
			.create();
		
		listView.setOnItemClickListener( new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id )
			{
				if ( position == Const.ONE_PLAYER) {
					//Log.v(TAG, "ListView:OnItemClick: 1 Player Game Chosen");
					gameView.getGameThread().newGame(Const.ONE_PLAYER);
				}
				else if ( position == Const.TWO_PLAYERS) {
					//Log.v(TAG, "ListView:OnItemClick: 2 Player Game Chosen");
					gameView.getGameThread().newGame(Const.TWO_PLAYERS);
				}
				// choice made so cancel the dialog
				ad.cancel();
			}
			
		});
		
		// show dialog
		ad.show();
	}
	
	private void showAboutDialog() {
		AlertDialog ad = new AlertDialog.Builder(this)
		.setTitle(R.string.app_name)
		.setMessage(R.string.copyright)
		.setPositiveButton(R.string.OK, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// do nothing
			}	
		})
		.create();
		ad.show();
	}
	
}