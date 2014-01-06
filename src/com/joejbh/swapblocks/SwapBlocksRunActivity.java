package com.joejbh.swapblocks;

import com.joejbh.swapblocks.R;

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

	@Override
	protected void onResume() {
		super.onResume();
		
		/*
		 * Initializing variables for mySwapBlockSurface here rather than onCreate because since the view is in the
		 * xml file, it will not inflate until after onCreate.
		 */
		
		mySwapBlocksSurface = (SwapBlocksSurface) findViewById(R.id.swapBlockSurface);
		mySwapBlocksSurface.setOnTouchListener(this);
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
}
