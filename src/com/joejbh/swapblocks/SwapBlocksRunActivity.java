package com.joejbh.swapblocks;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Point;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class SwapBlocksRunActivity extends Activity implements OnTouchListener {


	Display display;
	Point size = new Point();
	final static String displayIssues = "DISPLAY";

	SwapBlocksSurface mySwapBlocksSurface;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_draw_test);		
	}

	@Override
	protected void onPause() {
		super.onPause();
		mySwapBlocksSurface.pause();
	}

	
	/* 
	 * Initializing variables for mySwapBlockSurface here rather than onCreate
	 * Since the view is in the xml file, it will not inflate until after onCreate.
	 */
	
	@Override
	protected void onResume() {
		super.onResume();
		
		mySwapBlocksSurface = (SwapBlocksSurface) findViewById(R.id.swapBlockSurface);
		
		mySwapBlocksSurface.setOnTouchListener(this);
		mySwapBlocksSurface.resume();
		
	}

	public boolean onTouch(View view, MotionEvent event) {

		// Checks if the surface is busy.
		if (mySwapBlocksSurface.isFreeToMove()) {
			
			
			switch (event.getActionMasked()) {
			
			// ACTION_DOWN is just the first finger to go down.  Therefore, we do not increment the number 
			// of fingers. We simply set it to one.  ACTION_POINTER_DOWN is for extra fingers.
			case MotionEvent.ACTION_DOWN:
				
				mySwapBlocksSurface.setNumberOfFingers(1);
				
				mySwapBlocksSurface.setDownX(event.getX(0));
				mySwapBlocksSurface.setDownY(event.getY(0));

				break;

			case MotionEvent.ACTION_UP:
				
				// reduceNumberOfFingers works with multiple fingers, and is actually unnecessary.
				// However, I keep it for possible future use.
				mySwapBlocksSurface.reduceNumberOfFingers();
				
				mySwapBlocksSurface.setUpX(event.getX(0));
				mySwapBlocksSurface.setUpY(event.getY(0));

				// Prep variables necessary for movement.
				mySwapBlocksSurface.setMovementVars();
				
				// Start moving the blocks
				mySwapBlocksSurface.phaseHandleMove();
				
				break;
				
			}
		}
		return true;
	}	
}
