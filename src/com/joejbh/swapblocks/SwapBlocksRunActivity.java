package com.joejbh.swapblocks;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import com.joejbh.swapblock.R;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;

public class SwapBlocksRunActivity extends Activity implements OnTouchListener {

	boolean phaseDebugIsOn = false;
	boolean matchCheckDebugIsOn = false;
	boolean tripletColorDebugIsOn = false;
	boolean moveCounterDebugIsOn = false;
	boolean phaseStepDebugIsOn = false;

	float downX, downY;
	float upX, upY;

	Display display;
	Point size = new Point();
	final static String displayIssues = "DISPLAY";

	int blockWidth, blockHeight;

	Point myMatrixCoordinates[][];

	int moveCounter = 123; // This is the default number for when not to use
							// moveCounter
	int destroyCounter = 123;
	int shrinkCounter = 0;

	MediaPlayer poofSound;
	MediaPlayer shrinkSound;
	boolean shrinkSoundPlayed = false;
	boolean poofSoundPlayed = false;

	float movementPixels = 10; // This determines the speed of movement of the
								// blocks. Higher = faster. Must be divisible
								// into 100
	int matrixRows, matrixColumns;
	int numberOfFingers = 0;

	boolean swapInMotion = false;
	boolean reversingVars = false;
	boolean reverseInMotion = false;
	boolean tripletSearchInSession = false;
	boolean finishedFirstRootSearch = false;
	boolean finishedSecondRootSearch = false;

	int numberOfColors = 4;

	// Movement Variables
	private static final int FINISHED_MOVING = 1;

	int movementDirection = 0;

	private static final int MOVE_NULL = 0;
	private static final int MOVE_LEFT = 1;
	private static final int MOVE_RIGHT = 2;
	private static final int MOVE_UP = 3;
	private static final int MOVE_DOWN = 4;

	int activeCellRow = 999;
	int activeCellColumn = 999;

	private static final int ACTIVE_CELL_NULL = 999;
	// End Movement Variables

	// Phase Variables
	int movementPhase = 0;

	private static final int PHASE_NULL = 0;
	private static final int PHASE_HANDLE_MOVE = 1;
	private static final int PHASE_SWAP_IN_MATRIX = 2;
	private static final int PHASE_TRIPLET_SEARCH = 3;
	private static final int PHASE_IS_TRIPLET_FOUND = 4;
	private static final int PHASE_REVERSE_MVMNT_VARS = 5;
	private static final int PHASE_REVERSE_HANDLE_MOVE = 6;
	private static final int PHASE_RESWAP_IN_MATRIX = 7;
	private static final int PHASE_DROP = 8;
	private static final int PHASE_DONE_MOVE = 9;

	int phaseStep = 0;
	private static final int PHASE_STEP_ONE = 1;
	private static final int PHASE_STEP_TWO = 2;
	private static final int PHASE_STEP_THREE = 3;
	private static final int PHASE_STEP_FOUR = 4;
	// End Phase Variables

	boolean swapSuccess = false;
	boolean freeToMove = true;
	boolean tripletFound = false;

	String[] tripletColor = { "none", "none" };

	Bitmap bitmapG;
	Handler findDropHandler;
	FindDrops myFindDrops = new FindDrops();
	int[] dropColumns;
	private static final int NO_DROP = 99;

	Block tempBlock;

	SwapBlocksSurface mySwapBlocksSurface;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		display = getWindowManager().getDefaultDisplay();

		size = getSize(display);

		blockWidth = size.x / 5;
		blockHeight = blockWidth;

		matrixColumns = 5;
		matrixRows = (size.y / blockHeight) - 1;

		Log.i("READ ME", "Width = " + size.y);
		Log.i("READ ME", "Height = " + size.x);

		poofSound = MediaPlayer.create(this, R.raw.poof);
		shrinkSound = MediaPlayer.create(this, R.raw.shrink);

		mySwapBlocksSurface = new SwapBlocksSurface(this);
		mySwapBlocksSurface.setOnTouchListener(this);

		downX = downY = 0;
		upX = upY = 0;

		setContentView(mySwapBlocksSurface);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mySwapBlocksSurface.pause();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mySwapBlocksSurface.resume();
	}

	public boolean onTouch(View view, MotionEvent event) {

		if (freeToMove) {
			switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				numberOfFingers = 1;
				downX = event.getX(0);
				downY = event.getY(0);
				break;

			case MotionEvent.ACTION_MOVE:
				upX = event.getX(0);
				upY = event.getY(0);
				moveCounter = (int) (blockWidth / movementPixels);
				break;

			case MotionEvent.ACTION_UP:

				movementPhase = PHASE_HANDLE_MOVE;
				setMovementVars();
				numberOfFingers--;
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

	public Block[][] startGame(int rows, int columns) {

		Bitmap bitmap;
		Block[][] blockMatrix = new Block[rows][columns];
		myMatrixCoordinates = new Point[rows][columns];

		Random randomGenerator = new Random();
		int randomInt = 0;

		// Loop through the matrix and fill it with blocks
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < columns; c++) {

				randomInt = 1 + randomGenerator.nextInt(numberOfColors);

				// Randomly determine the color of the block and do appropriate
				// assignments.
				switch (randomInt) {
				case 1:
					bitmap = BitmapFactory.decodeResource(getResources(),
							R.drawable.block_red);
					bitmap = Bitmap.createScaledBitmap(bitmap, blockWidth,
							blockHeight, true); // Resize background to fit the
												// screen.
					blockMatrix[r][c] = new Block(bitmap, c * blockWidth, r
							* blockHeight, "Red");
					myMatrixCoordinates[r][c] = new Point(c * blockWidth, r
							* blockHeight);
					break;
				case 2:
					bitmap = BitmapFactory.decodeResource(getResources(),
							R.drawable.block_green);
					bitmap = Bitmap.createScaledBitmap(bitmap, blockWidth,
							blockHeight, true); // Resize background to fit the
												// screen.
					blockMatrix[r][c] = new Block(bitmap, c * blockWidth, r
							* blockHeight, "Green");
					myMatrixCoordinates[r][c] = new Point(c * blockWidth, r
							* blockHeight);
					break;
				case 3:
					bitmap = BitmapFactory.decodeResource(getResources(),
							R.drawable.block_blue);
					bitmap = Bitmap.createScaledBitmap(bitmap, blockWidth,
							blockHeight, true); // Resize background to fit the
												// screen.
					blockMatrix[r][c] = new Block(bitmap, c * blockWidth, r
							* blockHeight, "Blue");
					myMatrixCoordinates[r][c] = new Point(c * blockWidth, r
							* blockHeight);
					break;
				case 4:
					bitmap = BitmapFactory.decodeResource(getResources(),
							R.drawable.block_yellow);
					bitmap = Bitmap.createScaledBitmap(bitmap, blockWidth,
							blockHeight, true); // Resize background to fit the
												// screen.
					blockMatrix[r][c] = new Block(bitmap, c * blockWidth, r
							* blockHeight, "Yellow");
					myMatrixCoordinates[r][c] = new Point(c * blockWidth, r
							* blockHeight);
					break;
				}
			}
		}

		return blockMatrix;
	}

	public void setMovementVars() {
		float xChange, yChange;

		xChange = upX - downX;
		yChange = upY - downY;

		activeCellRow = (int) (downY / blockHeight);
		activeCellColumn = (int) (downX / blockWidth);

		// If there was more movement in X
		if (Math.abs(xChange) >= Math.abs(yChange)) {

			// If the movement in X was positive, move to the right
			if (xChange > 0) {

				// If the attempt to move to the right doesn't happen in the
				// right-most column
				if (activeCellColumn < matrixColumns - 1) {
					movementDirection = MOVE_RIGHT;
				}

				// Else if the attempt to move right takes place in the
				// right-most cell, end the move
				else {
					movementPhase = PHASE_DONE_MOVE;
				}
			}

			// Else if the X movement was negative, move to the left
			else {

				// If the attempt to move to the left doesn't happen in the
				// left-most column
				if (activeCellColumn > 0) {
					movementDirection = MOVE_LEFT;
				} else {
					movementPhase = PHASE_DONE_MOVE;
				}
			}
		}

		// If there was more movement in Y
		else {

			// If the movement in Y was positive, move down
			if (yChange > 0) {

				// If the attempt to move down doesn't happen in the bottom cell
				if (activeCellRow < matrixRows - 1) {
					movementDirection = MOVE_DOWN;
				}

				// Else if the attempt to move takes place in the bottom cell,
				// end the move
				else {
					movementPhase = PHASE_DONE_MOVE;
				}
			}

			// Else if the movement in Y was negative, move up
			else {

				// If the attempt to move down doesn't happen in the top cell
				if (activeCellRow > 0) {
					movementDirection = MOVE_UP;
				} else {
					movementPhase = PHASE_DONE_MOVE;
				}
			}
		}
	}

	public void reverseMovementVars() {

		switch (movementDirection) {

		case MOVE_LEFT:
			movementDirection = MOVE_RIGHT;
			break;

		case MOVE_RIGHT:
			movementDirection = MOVE_LEFT;
			break;

		case MOVE_UP:
			movementDirection = MOVE_DOWN;
			break;

		case MOVE_DOWN:
			movementDirection = MOVE_UP;
			break;

		}

		movementPhase = PHASE_REVERSE_HANDLE_MOVE;
	}

	public void handleMove(Block myBlockMatrix[][]) {

		switch (movementDirection) {

		case MOVE_LEFT:
			myBlockMatrix[activeCellRow][activeCellColumn]
					.setX(myBlockMatrix[activeCellRow][activeCellColumn].getX()
							- movementPixels);
			myBlockMatrix[activeCellRow][activeCellColumn - 1]
					.setX(myBlockMatrix[activeCellRow][activeCellColumn - 1]
							.getX() + movementPixels);

			break;

		case MOVE_RIGHT:
			myBlockMatrix[activeCellRow][activeCellColumn]
					.setX(myBlockMatrix[activeCellRow][activeCellColumn].getX()
							+ movementPixels);
			myBlockMatrix[activeCellRow][activeCellColumn + 1]
					.setX(myBlockMatrix[activeCellRow][activeCellColumn + 1]
							.getX() - movementPixels);

			break;

		case MOVE_UP:
			myBlockMatrix[activeCellRow][activeCellColumn]
					.setY(myBlockMatrix[activeCellRow][activeCellColumn].getY()
							- movementPixels);
			myBlockMatrix[activeCellRow - 1][activeCellColumn]
					.setY(myBlockMatrix[activeCellRow - 1][activeCellColumn]
							.getY() + movementPixels);

			break;

		case MOVE_DOWN:
			myBlockMatrix[activeCellRow][activeCellColumn]
					.setY(myBlockMatrix[activeCellRow][activeCellColumn].getY()
							+ movementPixels);
			myBlockMatrix[activeCellRow + 1][activeCellColumn]
					.setY(myBlockMatrix[activeCellRow + 1][activeCellColumn]
							.getY() - movementPixels);

			break;

		}

		if (moveCounter == FINISHED_MOVING - 1) {

			switch (movementDirection) {

			case MOVE_LEFT:
				myBlockMatrix[activeCellRow][activeCellColumn]
						.setX(myMatrixCoordinates[activeCellRow][activeCellColumn - 1].x);
				myBlockMatrix[activeCellRow][activeCellColumn - 1]
						.setX(myMatrixCoordinates[activeCellRow][activeCellColumn].x);

				break;

			case MOVE_RIGHT:
				myBlockMatrix[activeCellRow][activeCellColumn]
						.setX(myMatrixCoordinates[activeCellRow][activeCellColumn + 1].x);
				myBlockMatrix[activeCellRow][activeCellColumn + 1]
						.setX(myMatrixCoordinates[activeCellRow][activeCellColumn].x);

				break;

			case MOVE_UP:
				myBlockMatrix[activeCellRow][activeCellColumn]
						.setY(myMatrixCoordinates[activeCellRow - 1][activeCellColumn].y);
				myBlockMatrix[activeCellRow - 1][activeCellColumn]
						.setY(myMatrixCoordinates[activeCellRow][activeCellColumn].y);

				break;

			case MOVE_DOWN:
				myBlockMatrix[activeCellRow][activeCellColumn]
						.setY(myMatrixCoordinates[activeCellRow + 1][activeCellColumn].y);
				myBlockMatrix[activeCellRow + 1][activeCellColumn]
						.setY(myMatrixCoordinates[activeCellRow][activeCellColumn].y);

				break;

			}

			if (movementPhase == PHASE_HANDLE_MOVE) {
				movementPhase = PHASE_SWAP_IN_MATRIX;
			}

			else if (movementPhase == PHASE_REVERSE_HANDLE_MOVE) {
				movementPhase = PHASE_RESWAP_IN_MATRIX;
			}
		}

		moveCounter--;
	}

	public void swapInMatrix(Block myBlockMatrix[][]) {

		switch (movementDirection) {

		case MOVE_LEFT:

			tempBlock = myBlockMatrix[activeCellRow][activeCellColumn];
			myBlockMatrix[activeCellRow][activeCellColumn] = myBlockMatrix[activeCellRow][activeCellColumn - 1];
			myBlockMatrix[activeCellRow][activeCellColumn - 1] = tempBlock;

			activeCellColumn = activeCellColumn - 1;

			break;

		case MOVE_RIGHT:

			tempBlock = myBlockMatrix[activeCellRow][activeCellColumn];
			myBlockMatrix[activeCellRow][activeCellColumn] = myBlockMatrix[activeCellRow][activeCellColumn + 1];
			myBlockMatrix[activeCellRow][activeCellColumn + 1] = tempBlock;

			activeCellColumn = activeCellColumn + 1;

			break;

		case MOVE_UP:

			tempBlock = myBlockMatrix[activeCellRow][activeCellColumn];
			myBlockMatrix[activeCellRow][activeCellColumn] = myBlockMatrix[activeCellRow - 1][activeCellColumn];
			myBlockMatrix[activeCellRow - 1][activeCellColumn] = tempBlock;

			activeCellRow = activeCellRow - 1;

			break;

		case MOVE_DOWN:

			tempBlock = myBlockMatrix[activeCellRow][activeCellColumn];
			myBlockMatrix[activeCellRow][activeCellColumn] = myBlockMatrix[activeCellRow + 1][activeCellColumn];
			myBlockMatrix[activeCellRow + 1][activeCellColumn] = tempBlock;

			activeCellRow = activeCellRow + 1;

			break;
		}

		// Once block implosion enabled, delete line below and remove comment
		// block below
		movementPhase = PHASE_DONE_MOVE;

		//
		/*
		 * if(movementPhase == PHASE_SWAP_IN_MATRIX){ movementPhase =
		 * PHASE_TRIPLET_SEARCH; }else if(movementPhase ==
		 * PHASE_RESWAP_IN_MATRIX){ movementPhase = PHASE_DONE_MOVE; }
		 */

	}

	public boolean tripletSearchGoingUp(Block myBlockMatrix[][], int cellRow,
			int cellColumn) {

		int horizontalTripletCount = 0;
		int verticalTripletCount = 0;

		String currentColor;

		currentColor = myBlockMatrix[cellRow][cellColumn].getColor();

		myBlockMatrix[cellRow][cellColumn].setSearched(true);

		// Look LEFT one Column
		if (cellColumn != 0) {
			if (currentColor == myBlockMatrix[cellRow][cellColumn - 1]
					.getColor()) {

				if (!myBlockMatrix[cellRow][cellColumn - 1].getSearched()) {
					tripletSearchGoingLeft(myBlockMatrix, cellRow,
							cellColumn - 1);
				}

				horizontalTripletCount++;

				if (matchCheckDebugIsOn) {
					Log.i("READ ME",
							"Match to Left and horizontalTripletCount = "
									+ horizontalTripletCount);
				}
			}
		}

		// Look RIGHT one Column
		if (cellColumn != matrixColumns - 1) {
			if (currentColor == myBlockMatrix[cellRow][cellColumn + 1]
					.getColor()) {

				if (!myBlockMatrix[cellRow][cellColumn + 1].getSearched()) {
					tripletSearchGoingRight(myBlockMatrix, cellRow,
							cellColumn + 1);
				}

				horizontalTripletCount++;

				if (matchCheckDebugIsOn) {
					Log.i("READ ME",
							"Match to Right and horizontalTripletCount = "
									+ horizontalTripletCount);
				}
			}
		}

		// Look UP one row
		if (cellRow != 0) {
			if (currentColor == myBlockMatrix[cellRow - 1][cellColumn]
					.getColor()) {

				if (!myBlockMatrix[cellRow - 1][cellColumn].getSearched()) {
					tripletSearchGoingUp(myBlockMatrix, cellRow - 1, cellColumn);
				}

				verticalTripletCount++;

				if (matchCheckDebugIsOn) {
					Log.i("READ ME", "Match Above and verticalTripletCount = "
							+ verticalTripletCount);
				}
			}
		}

		// Look DOWN one row
		if (cellRow != matrixRows - 1) {
			if (currentColor == myBlockMatrix[cellRow + 1][cellColumn]
					.getColor()) {

				// tripletSearchGoingDown(myBlockMatrix, cellRow+1, cellColumn);

				verticalTripletCount++;

				if (matchCheckDebugIsOn) {
					Log.i("READ ME", "Match Below and verticalTripletCount = "
							+ verticalTripletCount);
				}
			}
		}

		if (verticalTripletCount == 2 || horizontalTripletCount == 2) {

			if (tripletColorDebugIsOn)
				Log.i("READ ME", "Found a triplet in the color of "
						+ currentColor);

			tripletFound = true;
			destroyCounter = (int) (blockWidth / movementPixels);
			shrinkCounter = (int) (blockWidth / movementPixels);

			if (tripletColor[0] == "none") {
				tripletColor[0] = currentColor;
			} else if (tripletColor[0] != currentColor) {
				tripletColor[1] = currentColor;
			}
		}

		return true;
	}

	public boolean tripletSearchGoingDown(Block myBlockMatrix[][], int cellRow,
			int cellColumn) {

		int horizontalTripletCount = 0;
		int verticalTripletCount = 0;

		String currentColor;

		myBlockMatrix[cellRow][cellColumn].setSearched(true);

		currentColor = myBlockMatrix[cellRow][cellColumn].getColor();

		// Look LEFT one Column
		if (cellColumn != 0) {
			if (currentColor == myBlockMatrix[cellRow][cellColumn - 1]
					.getColor()) {

				if (!myBlockMatrix[cellRow][cellColumn - 1].getSearched()) {
					tripletSearchGoingLeft(myBlockMatrix, cellRow,
							cellColumn - 1);
				}

				horizontalTripletCount++;

				if (matchCheckDebugIsOn) {
					Log.i("READ ME",
							"Match to Left and horizontalTripletCount = "
									+ horizontalTripletCount);
				}
			}
		}

		// Look RIGHT one Column
		if (cellColumn != matrixColumns - 1) {
			if (currentColor == myBlockMatrix[cellRow][cellColumn + 1]
					.getColor()) {

				if (!myBlockMatrix[cellRow][cellColumn + 1].getSearched()) {
					tripletSearchGoingRight(myBlockMatrix, cellRow,
							cellColumn + 1);
				}

				horizontalTripletCount++;

				if (matchCheckDebugIsOn) {
					Log.i("READ ME",
							"Match to Right and horizontalTripletCount = "
									+ horizontalTripletCount);
				}
			}
		}

		// Look UP one row
		if (cellRow != 0) {
			if (currentColor == myBlockMatrix[cellRow - 1][cellColumn]
					.getColor()) {

				// tripletSearchGoingUp(myBlockMatrix, cellRow-1, cellColumn);

				verticalTripletCount++;

				if (matchCheckDebugIsOn) {
					Log.i("READ ME", "Match Above and verticalTripletCount = "
							+ verticalTripletCount);
				}
			}
		}

		// Look DOWN one row
		if (cellRow != matrixRows - 1) {
			if (currentColor == myBlockMatrix[cellRow + 1][cellColumn]
					.getColor()) {

				if (!myBlockMatrix[cellRow + 1][cellColumn].getSearched()) {
					tripletSearchGoingDown(myBlockMatrix, cellRow + 1,
							cellColumn);
				}

				verticalTripletCount++;

				if (matchCheckDebugIsOn) {
					Log.i("READ ME", "Match Below and verticalTripletCount = "
							+ verticalTripletCount);
				}
			}
		}

		if (verticalTripletCount == 2 || horizontalTripletCount == 2) {

			if (tripletColorDebugIsOn)
				Log.i("READ ME", "Found a triplet in the color of "
						+ currentColor);

			tripletFound = true;
			destroyCounter = (int) (blockWidth / movementPixels);
			shrinkCounter = (int) (blockWidth / movementPixels);

			if (tripletColor[0] == "none") {
				tripletColor[0] = currentColor;
			} else if (tripletColor[0] != currentColor) {
				tripletColor[1] = currentColor;
			}
		}

		return true;
	}

	public boolean tripletSearchGoingLeft(Block myBlockMatrix[][], int cellRow,
			int cellColumn) {

		int horizontalTripletCount = 0;
		int verticalTripletCount = 0;

		String currentColor;

		myBlockMatrix[cellRow][cellColumn].setSearched(true);

		currentColor = myBlockMatrix[cellRow][cellColumn].getColor();

		// Look LEFT one Column
		if (cellColumn != 0) {
			if (currentColor == myBlockMatrix[cellRow][cellColumn - 1]
					.getColor()) {

				if (!myBlockMatrix[cellRow][cellColumn - 1].getSearched()) {
					tripletSearchGoingLeft(myBlockMatrix, cellRow,
							cellColumn - 1);
				}

				horizontalTripletCount++;

				if (matchCheckDebugIsOn) {
					Log.i("READ ME",
							"Match to Left and horizontalTripletCount = "
									+ horizontalTripletCount);
				}
			}
		}

		// Look RIGHT one Column
		if (cellColumn != matrixColumns - 1) {
			if (currentColor == myBlockMatrix[cellRow][cellColumn + 1]
					.getColor()) {

				// tripletSearchGoingRight(myBlockMatrix, cellRow,
				// cellColumn+1);
				horizontalTripletCount++;

				if (matchCheckDebugIsOn) {
					Log.i("READ ME",
							"Match to Right and horizontalTripletCount = "
									+ horizontalTripletCount);
				}
			}
		}

		// Look UP one row
		if (cellRow != 0) {
			if (currentColor == myBlockMatrix[cellRow - 1][cellColumn]
					.getColor()) {

				if (!myBlockMatrix[cellRow - 1][cellColumn].getSearched()) {
					tripletSearchGoingUp(myBlockMatrix, cellRow - 1, cellColumn);
				}
				verticalTripletCount++;

				if (matchCheckDebugIsOn) {
					Log.i("READ ME", "Match Above and verticalTripletCount = "
							+ verticalTripletCount);
				}
			}
		}

		// Look DOWN one row
		if (cellRow != matrixRows - 1) {
			if (currentColor == myBlockMatrix[cellRow + 1][cellColumn]
					.getColor()) {

				if (!myBlockMatrix[cellRow + 1][cellColumn].getSearched()) {
					tripletSearchGoingDown(myBlockMatrix, cellRow + 1,
							cellColumn);
				}

				verticalTripletCount++;

				if (matchCheckDebugIsOn) {
					Log.i("READ ME", "Match Below and verticalTripletCount = "
							+ verticalTripletCount);
				}
			}
		}

		if (verticalTripletCount == 2 || horizontalTripletCount == 2) {

			if (tripletColorDebugIsOn)
				Log.i("READ ME", "Found a triplet in the color of "
						+ currentColor);

			tripletFound = true;
			destroyCounter = (int) (blockWidth / movementPixels);
			shrinkCounter = (int) (blockWidth / movementPixels);

			if (tripletColor[0] == "none") {
				tripletColor[0] = currentColor;
			} else if (tripletColor[0] != currentColor) {
				tripletColor[1] = currentColor;
			}
		}

		return true;
	}

	public boolean tripletSearchGoingRight(Block myBlockMatrix[][],
			int cellRow, int cellColumn) {

		int horizontalTripletCount = 0;
		int verticalTripletCount = 0;

		String currentColor;

		myBlockMatrix[cellRow][cellColumn].setSearched(true);

		currentColor = myBlockMatrix[cellRow][cellColumn].getColor();

		// Look LEFT one Column
		if (cellColumn != 0) {
			if (currentColor == myBlockMatrix[cellRow][cellColumn - 1]
					.getColor()) {

				// tripletSearchGoingLeft(myBlockMatrix, cellRow, cellColumn-1);

				horizontalTripletCount++;

				if (matchCheckDebugIsOn) {
					Log.i("READ ME",
							"Match to Left and horizontalTripletCount = "
									+ horizontalTripletCount);
				}
			}
		}

		// Look RIGHT one Column
		if (cellColumn != matrixColumns - 1) {
			if (currentColor == myBlockMatrix[cellRow][cellColumn + 1]
					.getColor()) {
				if (!myBlockMatrix[cellRow][cellColumn + 1].getSearched()) {
					tripletSearchGoingRight(myBlockMatrix, cellRow,
							cellColumn + 1);
				}

				horizontalTripletCount++;

				if (matchCheckDebugIsOn) {
					Log.i("READ ME",
							"Match to Right and horizontalTripletCount = "
									+ horizontalTripletCount);
				}
			}
		}

		// Look UP one row
		if (cellRow != 0) {
			if (currentColor == myBlockMatrix[cellRow - 1][cellColumn]
					.getColor()) {

				if (!myBlockMatrix[cellRow - 1][cellColumn].getSearched()) {
					tripletSearchGoingUp(myBlockMatrix, cellRow - 1, cellColumn);
				}

				verticalTripletCount++;

				if (matchCheckDebugIsOn) {
					Log.i("READ ME", "Match Above and verticalTripletCount = "
							+ verticalTripletCount);
				}
			}
		}

		// Look DOWN one row
		if (cellRow != matrixRows - 1) {
			if (currentColor == myBlockMatrix[cellRow + 1][cellColumn]
					.getColor()) {

				if (!myBlockMatrix[cellRow + 1][cellColumn].getSearched()) {
					tripletSearchGoingDown(myBlockMatrix, cellRow + 1,
							cellColumn);
				}

				verticalTripletCount++;

				if (matchCheckDebugIsOn) {
					Log.i("READ ME", "Match Below and verticalTripletCount = "
							+ verticalTripletCount);
				}
			}
		}

		if (verticalTripletCount == 2 || horizontalTripletCount == 2) {

			if (tripletColorDebugIsOn)
				Log.i("READ ME", "Found a triplet in the color of "
						+ currentColor);

			tripletFound = true;
			destroyCounter = (int) (blockWidth / movementPixels);
			shrinkCounter = (int) (blockWidth / movementPixels);

			if (tripletColor[0] == "none") {
				tripletColor[0] = currentColor;
			} else if (tripletColor[0] != currentColor) {
				tripletColor[1] = currentColor;
			}
		}

		return true;
	}

	public void destroyBlock(Block myBlockMatrix[][]) {

		Bitmap bitmap;

		if (shrinkSoundPlayed == false) {
			shrinkSound.start();
			shrinkSoundPlayed = true;
		}

		for (int r = 0; r < matrixRows; r++) {
			for (int c = 0; c < matrixColumns; c++) {

				if (myBlockMatrix[r][c].getSearched() == true) {

					if (tripletColorDebugIsOn) {
						Log.i("READ ME", "First Color = " + tripletColor[0]);
						Log.i("READ ME", "Second Color = " + tripletColor[1]);
					}

					if (myBlockMatrix[r][c].getSearched() == true
							&& (myBlockMatrix[r][c].getColor() == tripletColor[0] || myBlockMatrix[r][c]
									.getColor() == tripletColor[1])) {

						if (myBlockMatrix[r][c].getColor() == "Red") {

							bitmap = BitmapFactory.decodeResource(
									getResources(), R.drawable.block_red);
							bitmap = Bitmap.createScaledBitmap(bitmap,
									blockWidth - (shrinkCounter), blockHeight
											- (shrinkCounter), true);
							myBlockMatrix[r][c].setImage(bitmap);

							Log.i("READ ME", "Before, X is: "
									+ myBlockMatrix[r][c].getX());

							myBlockMatrix[r][c]
									.setX(myBlockMatrix[r][c].getX()
											+ (float) ((blockWidth / movementPixels) / 2));
							myBlockMatrix[r][c]
									.setY(myBlockMatrix[r][c].getY()
											+ (float) ((blockWidth / movementPixels) / 2));

							Log.i("READ ME", "After, X is: "
									+ myBlockMatrix[r][c].getX());

						}

						if (myBlockMatrix[r][c].getColor() == "Blue") {

							bitmap = BitmapFactory.decodeResource(
									getResources(), R.drawable.block_blue);
							bitmap = Bitmap.createScaledBitmap(bitmap,
									blockWidth - (shrinkCounter), blockHeight
											- (shrinkCounter), true); // Resize
																		// background
																		// to
																		// fit
																		// the
																		// screen.
							myBlockMatrix[r][c].setImage(bitmap);

							Log.i("READ ME", "Before, X is: "
									+ myBlockMatrix[r][c].getX());

							myBlockMatrix[r][c]
									.setX(myBlockMatrix[r][c].getX()
											+ (float) ((blockWidth / movementPixels) / 2));
							myBlockMatrix[r][c]
									.setY(myBlockMatrix[r][c].getY()
											+ (float) ((blockWidth / movementPixels) / 2));

							Log.i("READ ME", "After, X is: "
									+ myBlockMatrix[r][c].getX());

						}

						if (myBlockMatrix[r][c].getColor() == "Green") {

							bitmap = BitmapFactory.decodeResource(
									getResources(), R.drawable.block_green);
							bitmap = Bitmap.createScaledBitmap(bitmap,
									blockWidth - (shrinkCounter), blockHeight
											- (shrinkCounter), true); // Resize
																		// background
																		// to
																		// fit
																		// the
																		// screen.
							myBlockMatrix[r][c].setImage(bitmap);

							Log.i("READ ME", "Before, X is: "
									+ myBlockMatrix[r][c].getX());

							myBlockMatrix[r][c]
									.setX(myBlockMatrix[r][c].getX()
											+ (float) ((blockWidth / movementPixels) / 2));
							myBlockMatrix[r][c]
									.setY(myBlockMatrix[r][c].getY()
											+ (float) ((blockWidth / movementPixels) / 2));

							Log.i("READ ME", "After, X is: "
									+ myBlockMatrix[r][c].getX());

						}

						if (myBlockMatrix[r][c].getColor() == "Yellow") {

							bitmap = BitmapFactory.decodeResource(
									getResources(), R.drawable.block_yellow);
							bitmap = Bitmap.createScaledBitmap(bitmap,
									blockWidth - (shrinkCounter), blockHeight
											- (shrinkCounter), true); // Resize
																		// background
																		// to
																		// fit
																		// the
																		// screen.
							myBlockMatrix[r][c].setImage(bitmap);

							Log.i("READ ME", "Before, X is: "
									+ myBlockMatrix[r][c].getX());

							myBlockMatrix[r][c]
									.setX(myBlockMatrix[r][c].getX()
											+ (float) ((blockWidth / movementPixels) / 2));
							myBlockMatrix[r][c]
									.setY(myBlockMatrix[r][c].getY()
											+ (float) ((blockWidth / movementPixels) / 2));

							Log.i("READ ME", "After, X is: "
									+ myBlockMatrix[r][c].getX());
						}
					}
				}
			}
		}

		// +2 because we don't really have to wait until the block is totally
		// nothing.
		if (destroyCounter == FINISHED_MOVING) {

			// Turn the destroyed blocks into puffs of smoke.
			for (int r = 0; r < matrixRows; r++) {
				for (int c = 0; c < matrixColumns; c++) {

					if (myBlockMatrix[r][c].getSearched() == true
							&& (myBlockMatrix[r][c].getColor() == tripletColor[0] || myBlockMatrix[r][c]
									.getColor() == tripletColor[1])) {

						if (myBlockMatrix[r][c].getSearched() == true) {

							bitmap = BitmapFactory.decodeResource(
									getResources(), R.drawable.block_destroyed);
							bitmap = Bitmap.createScaledBitmap(bitmap,
									blockWidth, blockHeight, true);
							myBlockMatrix[r][c].setImage(bitmap);
							myBlockMatrix[r][c]
									.setX(myMatrixCoordinates[r][c].x);
							myBlockMatrix[r][c]
									.setY(myMatrixCoordinates[r][c].y);
						}
					}
				}
			}
			moveCounter = (int) (blockWidth / movementPixels);
			movementPhase = PHASE_DROP;
			phaseStep = PHASE_STEP_ONE;
		}

		shrinkCounter = shrinkCounter + (int) (blockWidth / movementPixels);
		destroyCounter--;
	}

	public void doneMove(Block myBlockMatrix[][]) {
		downX = downY = upX = upY = 0;
		moveCounter = 123;

		destroyCounter = 123;
		shrinkCounter = 0;
		shrinkSoundPlayed = false;
		poofSoundPlayed = false;

		freeToMove = true;
		tripletFound = false;
		swapInMotion = false;

		movementDirection = MOVE_NULL;
		activeCellRow = ACTIVE_CELL_NULL;
		activeCellColumn = ACTIVE_CELL_NULL;
		movementPhase = PHASE_NULL;

		reversingVars = false;
		reverseInMotion = false;

		tripletColor[0] = "none";
		tripletColor[1] = "none";

		phaseStep = PHASE_STEP_ONE;

		tripletSearchInSession = false;
		finishedFirstRootSearch = false;
		finishedSecondRootSearch = false;

		for (int i = 0; i < matrixRows; i++) {
			for (int j = 0; j < matrixColumns; j++) {
				myBlockMatrix[i][j].setSearched(false);
			}
		}

		for (int i = 0; i < matrixColumns; i++)
			dropColumns[i] = NO_DROP;
	}

	public class Block {
		float blockX, blockY;

		String blockColor;
		Bitmap blockImage;

		boolean searched = false;

		public Block(Bitmap blockImage, float blockX, float blockY,
				String blockColor) {
			this.blockImage = blockImage;
			this.blockX = blockX;
			this.blockY = blockY;
			this.blockColor = blockColor;
		}

		// Setters
		void setImage(Bitmap blockImage) {
			this.blockImage = blockImage;
		}

		void setX(float blockX) {
			this.blockX = blockX;
		}

		void setY(float blockY) {
			this.blockY = blockY;
		}

		void setColor(String blockColor) {
			this.blockColor = blockColor;
		}

		void setSearched(boolean searched) {
			this.searched = searched;
		}

		// Getters
		Bitmap getImage() {
			return blockImage;
		}

		float getX() {
			return blockX;
		}

		float getY() {
			return blockY;
		}

		String getColor() {
			return blockColor;
		}

		boolean getSearched() {
			return searched;
		}
	}

	public class SwapBlocksSurface extends SurfaceView implements Runnable {

		TextView scoreTextView;

		SurfaceHolder ourHolder;
		Thread myThread = null;
		boolean isRunning = false;

		Block myBlockMatrix[][];

		public SwapBlocksSurface(Context context) {
			super(context);
			ourHolder = getHolder();
		}

		public void pause() {
			isRunning = false;
			while (true) {
				try {
					myThread.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
			myThread = null;
		}

		public void resume() {

			scoreTextView = (TextView) findViewById(R.id.scoreTextView);

			isRunning = true;
			dropColumns = new int[matrixColumns];

			for (int i = 0; i < matrixColumns; i++)
				dropColumns[i] = NO_DROP;

			myThread = new Thread(this);
			myThread.start();
		}

		public void run() {
			myBlockMatrix = startGame(matrixRows, matrixColumns);
			long startTime;

			// TODO Auto-generated method stub
			while (isRunning) {
				if (!ourHolder.getSurface().isValid())
					continue;

				Canvas canvas = ourHolder.lockCanvas();
				canvas.drawRGB(74, 92, 128);

				// This prints the full matrix of blocks
				for (int i = 0; i < matrixRows; i++) {
					for (int j = 0; j < matrixColumns; j++) {
						canvas.drawBitmap(myBlockMatrix[i][j].getImage(),
								myBlockMatrix[i][j].getX(),
								myBlockMatrix[i][j].getY(), null);
					}
				}

				// If movement has started, act according to what phase it is
				switch (movementPhase) {

				case PHASE_NULL:

					break;

				case PHASE_HANDLE_MOVE:
					if (phaseDebugIsOn) {
						Log.i("READ ME", "Starting Phase: PHASE_HANDLE_MOVE");
					}

					if (moveCounter != 123 && numberOfFingers == 0) {
						freeToMove = false;
						handleMove(myBlockMatrix);
					}
					break;

				case PHASE_SWAP_IN_MATRIX:

					if (phaseDebugIsOn) {
						Log.i("READ ME", "Starting Phase: PHASE_SWAP_IN_MATRIX");
					}

					if (swapInMotion == false) {
						swapInMotion = true;
						swapInMatrix(myBlockMatrix);
					}
					break;

				case PHASE_TRIPLET_SEARCH:

					if (phaseDebugIsOn) {
						Log.i("READ ME", "Starting Phase: PHASE_TRIPLET_SEARCH");
					}

					if (tripletSearchInSession == false) {
						swapInMotion = false;
						tripletSearchInSession = true;

						switch (movementDirection) {
						case MOVE_LEFT:

							finishedFirstRootSearch = tripletSearchGoingLeft(
									myBlockMatrix, activeCellRow,
									activeCellColumn);
							finishedSecondRootSearch = tripletSearchGoingRight(
									myBlockMatrix, activeCellRow,
									activeCellColumn + 1);

							break;

						case MOVE_RIGHT:

							finishedFirstRootSearch = tripletSearchGoingLeft(
									myBlockMatrix, activeCellRow,
									activeCellColumn - 1);
							finishedSecondRootSearch = tripletSearchGoingRight(
									myBlockMatrix, activeCellRow,
									activeCellColumn);

							break;

						case MOVE_UP:

							finishedFirstRootSearch = tripletSearchGoingUp(
									myBlockMatrix, activeCellRow,
									activeCellColumn);
							finishedSecondRootSearch = tripletSearchGoingDown(
									myBlockMatrix, activeCellRow + 1,
									activeCellColumn);

							break;

						case MOVE_DOWN:

							finishedFirstRootSearch = tripletSearchGoingUp(
									myBlockMatrix, activeCellRow - 1,
									activeCellColumn);
							finishedSecondRootSearch = tripletSearchGoingDown(
									myBlockMatrix, activeCellRow,
									activeCellColumn);

							break;
						}
					}

					if (finishedFirstRootSearch && finishedSecondRootSearch) {
						movementPhase = PHASE_IS_TRIPLET_FOUND;
					}

					break;

				case PHASE_IS_TRIPLET_FOUND:

					if (phaseDebugIsOn) {
						Log.i("READ ME",
								"Starting Phase PHASE_IS_TRIPLET_FOUND");
					}

					if (tripletFound) {

						if (destroyCounter != 123) {
							destroyBlock(myBlockMatrix);
						}
					}

					else {
						movementPhase = PHASE_REVERSE_MVMNT_VARS;
						moveCounter = (int) (blockWidth / movementPixels);
					}

					break;

				case PHASE_REVERSE_MVMNT_VARS:

					if (phaseDebugIsOn) {
						Log.i("READ ME", "Starting Phase 5");
					}

					if (reversingVars == false) {
						reversingVars = true;
						reverseMovementVars();
					}
					break;

				case PHASE_REVERSE_HANDLE_MOVE:

					if (phaseDebugIsOn) {
						Log.i("READ ME", "Starting Phase 6");
					}

					if (moveCounter != 123 && numberOfFingers == 0) {
						// Log.i("READ ME", "Move Counter is " + moveCounter);
						handleMove(myBlockMatrix);
					}
					break;

				case PHASE_RESWAP_IN_MATRIX:

					if (phaseDebugIsOn) {
						Log.i("READ ME", "Starting Phase 7");
						Log.i("READ ME", "reverseInMotion = " + reverseInMotion);
					}

					if (reverseInMotion == false) {
						reverseInMotion = true;
						swapInMatrix(myBlockMatrix);
					}
					break;

				case PHASE_DROP:

					if (phaseDebugIsOn)
						Log.i("READ ME", "Starting Drop Phase");

					if (phaseStepDebugIsOn)
						Log.i("READ ME", "phase step = " + phaseStep);

					startTime = System.nanoTime();

					while (System.nanoTime() - startTime < 1000000000) {
					}

					switch (phaseStep) {

					case PHASE_STEP_ONE: // Make the destroyed blocks DISAPPEAR.

						for (int r = 0; r < matrixRows; r++) {
							for (int c = 0; c < matrixColumns; c++) {

								if (myBlockMatrix[r][c].getSearched() == true
										&& (myBlockMatrix[r][c].getColor() == tripletColor[0] || myBlockMatrix[r][c]
												.getColor() == tripletColor[1])) {

									if (myBlockMatrix[r][c].getSearched() == true) {

										if (poofSoundPlayed == false) {
											poofSound.start();
											poofSoundPlayed = true;
										}

										bitmapG = BitmapFactory.decodeResource(
												getResources(),
												R.drawable.block_gone);
										bitmapG = Bitmap.createScaledBitmap(
												bitmapG, blockWidth,
												blockHeight, true);
										myBlockMatrix[r][c].setImage(bitmapG);
										/*
										 * Log.i("READ ME",
										 * "Description of disappearing block: "
										 * ); Log.i("READ ME", "x = " +
										 * myBlockMatrix[r][c].getX());
										 * Log.i("READ ME", "y = " +
										 * myBlockMatrix[r][c].getY());
										 * Log.i("READ ME", "Color = " +
										 * myBlockMatrix[r][c].getColor());
										 * Log.i("READ ME", "Searched = " +
										 * myBlockMatrix[r][c].getSearched());
										 */

									}
								}
							}
						}
						phaseStep = PHASE_STEP_TWO;
						break;

					case PHASE_STEP_TWO: // Run thread which FINDS which cells
											// are TO BE DROPPED.
											// This thread also does some
											// swapping of block designations.

						phaseStep = 0; // This will prevent this phase step from
										// running again until the thread is
										// finished.

						findDropHandler.post(new Runnable() {

							public void run() {
								myFindDrops.checkDrop(myBlockMatrix);

								phaseStep = PHASE_STEP_THREE;
								moveCounter = (int) (blockHeight / movementPixels);
							}
						});

						break;

					case PHASE_STEP_THREE: // This part does the DROP MOVEMENT

						if (moveCounterDebugIsOn)
							Log.i("READ ME", "Move Counter = " + moveCounter);

						// If moveCounter is still above 0, then go in here and
						// move the blocks
						if (moveCounter > FINISHED_MOVING - 1) {

							for (int dropColumn = 0; dropColumn < matrixColumns; dropColumn++) {
								if (dropColumns[dropColumn] != NO_DROP) {
									for (int dropRow = dropColumns[dropColumn]; dropRow >= 0; dropRow--) {
										myBlockMatrix[dropRow][dropColumn]
												.setY(myBlockMatrix[dropRow][dropColumn]
														.getY()
														+ movementPixels);
									}
								}
							}

							moveCounter--;

						}

						// If moveCounter is at zero, that means we're finished
						// moving the blocks. Now check if more need to be moved
						// or we're done.
						else if (moveCounter == FINISHED_MOVING - 1) {

							// Make sure that the columns are properly aligned
							for (int dropColumn = 0; dropColumn < matrixColumns; dropColumn++) {
								if (dropColumns[dropColumn] != NO_DROP) {
									for (int dropRow = dropColumns[dropColumn]; dropRow >= 0; dropRow--) {
										myBlockMatrix[dropRow][dropColumn]
												.setY(myMatrixCoordinates[dropRow][dropColumn].y);
									}
								}
							}

							// If any of the blocks are still blank
							for (int c = 0; c < matrixColumns; c++) {
								if (dropColumns[c] != 99) {
									phaseStep = PHASE_STEP_TWO;

									for (int j = 0; j < matrixColumns; j++)
										dropColumns[j] = NO_DROP;
									break;
								}
							}

							if (phaseStep != PHASE_STEP_TWO)
								movementPhase = PHASE_DONE_MOVE;
						}

						break;
					}

					break;

				case PHASE_DONE_MOVE:

					if (phaseDebugIsOn) {
						Log.i("READ ME", "Starting Done Move Phase");
					}

					doneMove(myBlockMatrix);
					movementPhase = MOVE_NULL;

					break;

				}

				ourHolder.unlockCanvasAndPost(canvas);
			}
		}
	}

	public class FindDrops implements Runnable {

		Thread myThread = null;

		// boolean isRunning = false;

		public FindDrops() {
			// isRunning = true;
			myThread = new Thread(this);
			myThread.start();
		}

		public void run() {
			// TODO Auto-generated method stub

			try {
				Looper.prepare();

				// threadLooper = Looper.myLooper();

				findDropHandler = new Handler();

				Looper.loop();

				Looper.myLooper().quit();

			} catch (Throwable t) {
				Log.e("READ ME", "halted due to an error", t);
			}
		}

		public void checkDrop(Block myBlockMatrix[][]) {

			Random randomGenerator = new Random();
			int randomInt = 0;
			Bitmap bitmap;

			// Loop through each column, starting from the bottom. Look for a
			// block that has been destroyed and note it as a dropColumn
			for (int c = 0; c < matrixColumns; c++) {
				for (int r = matrixRows - 1; r >= 0; r--) {
					if (myBlockMatrix[r][c].getSearched() == true
							&& (myBlockMatrix[r][c].getColor() == tripletColor[0] || myBlockMatrix[r][c]
									.getColor() == tripletColor[1])) {
						dropColumns[c] = r;

						// Once the block to be dropped has been found, put it
						// at the top, and reassign which Matrix coordinates
						// point to which Blocks.
						tempBlock = myBlockMatrix[r][c];
						while (r > 0) {
							myBlockMatrix[r][c] = myBlockMatrix[r - 1][c];
							r--;
						}

						randomInt = 1 + randomGenerator.nextInt(numberOfColors);

						// Randomly determine the color of the block and do
						// appropriate assignments.
						switch (randomInt) {
						case 1:
							tempBlock.setColor("Red");
							bitmap = BitmapFactory.decodeResource(
									getResources(), R.drawable.block_red);
							bitmap = Bitmap.createScaledBitmap(bitmap,
									blockWidth, blockHeight, true);
							tempBlock.setImage(bitmap);
							tempBlock.setSearched(false);
							break;
						case 2:
							tempBlock.setColor("Green");
							bitmap = BitmapFactory.decodeResource(
									getResources(), R.drawable.block_green);
							bitmap = Bitmap.createScaledBitmap(bitmap,
									blockWidth, blockHeight, true);
							tempBlock.setImage(bitmap);
							tempBlock.setSearched(false);
							break;
						case 3:
							tempBlock.setColor("Blue");
							bitmap = BitmapFactory.decodeResource(
									getResources(), R.drawable.block_blue);
							bitmap = Bitmap.createScaledBitmap(bitmap,
									blockWidth, blockHeight, true);
							tempBlock.setImage(bitmap);
							tempBlock.setSearched(false);
							break;
						case 4:
							tempBlock.setColor("Yellow");
							bitmap = BitmapFactory.decodeResource(
									getResources(), R.drawable.block_yellow);
							bitmap = Bitmap.createScaledBitmap(bitmap,
									blockWidth, blockHeight, true);
							tempBlock.setImage(bitmap);
							tempBlock.setSearched(false);
							break;
						}

						tempBlock.setY(0 - blockHeight);
						myBlockMatrix[0][c] = tempBlock;
						break;
					}
				}
			}
		}
	}
}
