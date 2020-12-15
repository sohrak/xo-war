package sohrakoff.cory.xow;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

	// reference to context so that drawables can be referenced
	private Context context;
	
	private static final String TAG = "GameView";
	
	// the thread handling the drawing and other time-based logic
	private GameThread gameThread;
	
	public GameView(final Context context, AttributeSet attrs) {
		super(context, attrs);
		
		this.context = context;
		
		// Register for surface callbacks.
		SurfaceHolder surfaceHolder = getHolder();
		surfaceHolder.addCallback(this);
		
		gameThread = new GameThread(context, surfaceHolder, new Handler() {
			@Override
            public void handleMessage(Message m) {
				((XOWar)context).setTitle(m.getData().getInt("titleId"));
            }
		});
		
		// want the view to be able to accept input events
		setFocusable(true);
	}
	
	public GameThread getGameThread() {
		return gameThread;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				//Log.v(TAG, "onTouchEvent: Action Down");
				gameThread.doTouch((int)event.getX(), (int)event.getY());
				return true;
			default:
				//Log.v(TAG, "Received unhandled TouchEvent");
				break;
		}
		return super.onTouchEvent(event);
	}
	
	/*
	 * Surface callbacks.
	 */
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		gameThread.setUpScale(width, height);
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// start thread now since surface is ready
		gameThread.setRunning(true);
		gameThread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// set thread running to false and wait for it to die
		// since surface will be destroyed after this call returns
        boolean retry = true;
        gameThread.setRunning(false);
        while (retry) {
            try {
                gameThread.join();
                retry = false;
            } catch (Exception e) {}
        }
        //Log.v(TAG, "Thread killed");
	}

}
