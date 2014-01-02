package com.joejbh.swapblocks;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.joejbh.swapblocks.R;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.app.Activity;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;

public class SwapBlocksRunActivity extends Activity implements OnTouchListener {


	Display display;
	Point size = new Point();
	final static String displayIssues = "DISPLAY";

	SwapBlocksSurface mySwapBlocksSurface;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.i("onCrete", "1");
		
		setContentView(R.layout.activity_draw_test);
		
		
		
		
		/*mySwapBlocksSurface.poofSound = MediaPlayer.create(this, R.raw.poof);
		mySwapBlocksSurface.shrinkSound = MediaPlayer.create(this, R.raw.shrink);
*/
		
		



//		setContentView(mySwapBlocksSurface);
		
	}

	@Override
	protected void onPause() {
		super.onPause();
		mySwapBlocksSurface.pause();
	}

	@Override
	protected void onResume() {
		super.onResume();
//		mySwapBlocksSurface.resume();
		
//		LinearLayout activityLayout = (LinearLayout) findViewById(R.layout.activity_draw_test);
		
		
	
		Log.i("onCrete", "2");
		mySwapBlocksSurface = (SwapBlocksSurface) findViewById(R.id.swapBlockSurface);
		Log.i("onCrete", "3");
			
//		mySwapBlocksSurface = new SwapBlocksSurface(this);
		
		mySwapBlocksSurface.setOnTouchListener(this);
		Log.i("onCrete", "4");
		display = getWindowManager().getDefaultDisplay();
		Log.i("onCrete", "5");
		size = getSize(display);
		Log.i("onCrete", "6");
		if (size.x > size.y)
			mySwapBlocksSurface.setMatrixColumns(10);
		else
			mySwapBlocksSurface.setMatrixColumns(5);

		
		mySwapBlocksSurface.setBlockWidth(size.x / mySwapBlocksSurface.getMatrixColumns());
		mySwapBlocksSurface.setBlockHeight(mySwapBlocksSurface.getBlockWidth());

		mySwapBlocksSurface.setMatrixRows( (size.y / mySwapBlocksSurface.getBlockHeight()) - 1 );

		Log.i("onCreate", "Width = " + size.y);
		Log.i("onCreate", "Height = " + size.x);
		
		mySwapBlocksSurface.resume();
	}

	public boolean onTouch(View view, MotionEvent event) {

		if (mySwapBlocksSurface.isFreeToMove()) {
			switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				
				mySwapBlocksSurface.setNumberOfFingers(1);
				
				mySwapBlocksSurface.setDownX(event.getX(0));
				mySwapBlocksSurface.setDownY(event.getY(0));

				break;

			case MotionEvent.ACTION_MOVE:
				
				mySwapBlocksSurface.setUpX(event.getX(0));
				mySwapBlocksSurface.setUpY(event.getY(0));

				mySwapBlocksSurface.setMoveCounter( (int) 
						(mySwapBlocksSurface.getBlockWidth() / 
						mySwapBlocksSurface.getMovementPixels()) );
				break;

			case MotionEvent.ACTION_UP:

				mySwapBlocksSurface.movementPhase = SwapBlocksSurface.PHASE_HANDLE_MOVE;
				mySwapBlocksSurface.setMovementVars();
				mySwapBlocksSurface.reduceNumberOfFingers();
				break;
			}
		}
		return true;
	}

	// Cope with deprecated getWidth() and getHeight() methods
	public Point getSize(Display xiDisplay) {
		Point outSize = new Point();
		boolean sizeFound = false;

		try {
			// Test if the new getSize() method is available
			Method newGetSize = Display.class.getMethod("getSize",
					new Class[] { Point.class });

			// No exception, so the new method is available
			Log.d(displayIssues, "Use getSize to find screen size");
			newGetSize.invoke(xiDisplay, outSize);
			sizeFound = true;
			Log.d(displayIssues, "Screen size is " + outSize.x + " x "
					+ outSize.y);
		}

		catch (NoSuchMethodException ex) {
			// This is the failure I expect when the deprecated APIs are not
			// available
			Log.d(displayIssues,
					"getSize not available - NoSuchMethodException");
		}

		catch (InvocationTargetException e) {
			Log.w(displayIssues,
					"getSize not available - InvocationTargetException");
		}

		catch (IllegalArgumentException e) {
			Log.w(displayIssues,
					"getSize not available - IllegalArgumentException");
		}

		catch (IllegalAccessException e) {
			Log.w(displayIssues,
					"getSize not available - IllegalAccessException");
		}

		if (!sizeFound) {
			Log.i(displayIssues,
					"Used deprecated methods as getSize not available");
			outSize = new Point(xiDisplay.getWidth(), xiDisplay.getHeight());
		}

		return outSize;
	}
	
}
