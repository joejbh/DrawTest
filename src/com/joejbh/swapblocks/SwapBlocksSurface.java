package com.joejbh.swapblocks;

import java.util.Random;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * The SwapBlockSurface is comprised of blocks.  When two blocks are swapped, the surface checks if either
 * of swapped blocks are touching a series of two or more blocks of the same color.  If so, the blocks explode
 * and are replaced by a cascade of new blocks from above.
 * 
 * The blocks are shown on screen, but there is also a matrix of coordinates in order to keep track of appropriate
 * places to draw the blocks.
 * 
 * The surface is a runnable that is in a continuous loop and is continuously drawing.  I have setup phases 
 * flags to instruct the loop as to which series of commands it should run at any given point.  
 * 
 * The Phases:
 * PHASE_HANDLE_MOVE: handle the movement of the blocks
 * PHASE_SWAP_IN_MATRIX: swap the block object designations within the coordinates matrix.
 * PHASE_TRIPLET_SEARCH: search for groups of three or more blocks in relation to the swapped blocks
 * PHASE_IS_TRIPLET_FOUND: handles actions if a triplet is found
 * PHASE_REVERSE_MVMNT_VARS: reverses variables if no triplet found
 * PHASE_REVERSE_HANDLE_MOVE: handle movement of swapping blocks back if no triplet found
 * PHASE_RESWAP_IN_MATRIX: swap blocks back to original location if no triplet was found
 * PHASE_DROP: triplet found, destroy blocks, new blocks fall from above.  This phase is broken into four steps flags.
 * PHASE_DONE_MOVE: 
 * 
 * @author Ibis
 *
 */
public class SwapBlocksSurface extends SurfaceView implements Runnable {

	private boolean phaseDebugIsOn = false;
	private boolean phaseStepDebugIsOn = true;
	private boolean matchCheckDebugIsOn = false;
	private boolean tripletColorDebugIsOn = false;
	private boolean moveCounterDebugIsOn = false;
	private boolean destroyBlockDebugIsOn = false;
	
	private boolean phaseDebugNotNoted = true;
	private boolean phaseStepDebugNotNoted = true;
	
	// Phase flags
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

	
	// A lot happens in drop phase, so I broke it into four steps
	private static final int PHASE_STEP_ONE = 1;
	private static final int PHASE_STEP_TWO = 2;
	private static final int PHASE_STEP_THREE = 3;

	// This matrix keeps track of where the positioning of settled blocks can be.
	// This variable is important because blocks are moved and shrink in
	// fractions of block sizes, which sometimes is not an integer. During a move or
	// shrinking, the intended final position may be lost, but this keeps record of appropriate placement.
	private Point myMatrixCoordinates[][];

	// Number of fingers recorded as touching the screen.  Never goes above 1, since there is no need to 
	// track additional fingers.
	public int numberOfFingers = 0;

	// Where the finger used for indicating movement touched the screen and lifted from the screen in x & y coords.
	private float downX = 0, downY = 0;
	private float upX = 0, upY = 0;
	
	// Flags used to indicate that a particular action is in progress and conflicting actions should not take place.
	private boolean swapInMotion = false;
	private boolean reversingVars = false;
	private boolean reverseInMotion = false;
	private boolean tripletSearchInSession = false;
	private boolean finishedFirstRootSearch = false;
	private boolean finishedSecondRootSearch = false;

	private Bitmap tmpBlockBmap;

	// Used for swapping blocks in matrix
	private Block tempBlock;
	
	private int numberOfColors = 4;
	
	private int blockWidth, blockHeight;
	
	
	
	// String for recording which blocks should be destroyed after finding triplets. 
	private String destroyTheseBlocks = "";
	
	// If triplets found, records which colors are triplets.
	private String[] tripletColor = { "none", "none" };
	
	// Whether or not a triplet has been found yet.
	private boolean tripletFound = false;
	
	
	
	// A count down variable for managing movement of blocks
	private int moveCounter = -1; 

	// A count down variable for managing the shrinking of blocks during destruction.
	private int destroyCounter = -1;
	
	// Shrink counter increases as destroyCounter decreases.  ShrinkCounter determines how to resize blocks
	// while they're shrinking to blow up.
	private int shrinkCounter = 0;

	
	
	// This determines the speed of movement of the blocks. Higher = faster. Must be divisible into 100
	private float movementPixels = 10; 

	private int matrixRows, matrixColumns;

	
	private MediaPlayer poofSound = MediaPlayer.create(getContext(), R.raw.poof);
	private MediaPlayer shrinkSound = MediaPlayer.create(getContext(), R.raw.shrink);
	
	private boolean shrinkSoundPlayed = false;
	private boolean poofSoundPlayed = false;
	

	// --------------------------
	// --- Movement Variables ---
	
	private static final int FINISHED_MOVING = 1;

	
	// movementDirection keeps track of the movement of the original block touched. 
	private int movementDirection = 0;

	// Flags for movementDirection
	private static final int MOVE_NULL = 0;
	private static final int MOVE_LEFT = 1;
	private static final int MOVE_RIGHT = 2;
	private static final int MOVE_UP = 3;
	private static final int MOVE_DOWN = 4;

	// The original cell touched prior to movement.
	private int activeCellRow = 999;
	private int activeCellColumn = 999;

	private static final int ACTIVE_CELL_NULL = 999;

	// --- End Movement Variables ---
	// ------------------------------

	
	
	// Phase Variables
	private int movementPhase = 0;

	private int phaseStep = 0;

	// Used for recording if the user can do another move yet.
	private boolean freeToMove = true;

	// End Phase Variables


	// An array of each column that will have blocks drop due to a block
	// disappearing in the column.
	private int[] dropColumns;
	private static final int NO_DROP = 99;

	
	private SurfaceHolder ourHolder;
	private Thread myThread = null;
	private boolean isRunning = false;

	private Block myBlockMatrix[][];
	
	// These parameters are required in order to use SwapBlockSurface in an XML file.
	public SwapBlocksSurface(Context context, AttributeSet attributeSet)
	{
	    super(context, attributeSet);
	    ourHolder = getHolder();
	}
	
	
	// When the activity pauses, the thread needs to pause also.  
	// The thread is joined and therefore stopped until further notice.
	public void pause() {
		Log.i("SwapBlockSurface State", "pause()");
		
		isRunning = false;
		while (true) {
			try {
				myThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			break;
		}
	}

	
	// The current version of the app doesn't bother recording the screen setup between onPause's
	// All default values are reset in resume().
	public void resume() {
		Log.i("SwapBlockSurface State", "resume()");

		isRunning = true;
		
		// Start a new thread even if this is after a pause since the screen may have been reoriented.
		// Normally, we would want to store the app state and re-display... but this is a demo.
		myThread = new Thread(this);
		myThread.start();
	}

	public void run() {
		// Force a delay in order to allow proper loading time.  Variable also used for tracking how long
		// phases take to run.
		long startTime, endTime;
		long stepStartTime = 0, stepEndTime = 0;
		startTime = System.nanoTime();
		while (System.nanoTime() - startTime < 1000000000) {}

		// The height and width of the surfaceView.
		int viewHeight = this.getHeight();
		int viewWidth = this.getWidth();
		
		matrixColumns = viewWidth/115;

		blockWidth = (viewWidth / matrixColumns);
		blockHeight = blockWidth;

		matrixRows = ( viewHeight / blockHeight);
		
		// initialize variables.
		myBlockMatrix = startGame(matrixRows, matrixColumns);

		dropColumns = new int[matrixColumns];
		for (int i = 0; i < matrixColumns; i++){
			dropColumns[i] = NO_DROP;
		}
		
		
		// Here is where the App continues to loop during runtime:
		while (isRunning) {
			
			if (!ourHolder.getSurface().isValid())
				continue;
			
			// Lock the canvas during calculations and then the canvas is unlocked and redrawn thereafter. 
			Canvas canvas = ourHolder.lockCanvas();
			
			// Background color
			canvas.drawRGB(49, 98, 122);
			
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
				startTime = System.nanoTime();
				if (phaseDebugIsOn && phaseDebugNotNoted) {
					Log.i("phaseDebug", "Starting Phase: PHASE_HANDLE_MOVE");
					phaseDebugNotNoted = false;
				}

				if (moveCounter != -1 && numberOfFingers == 0) {
					freeToMove = false;
					handleMove(myBlockMatrix);
				}
				break;

			case PHASE_SWAP_IN_MATRIX:

				endTime = System.nanoTime();
				
				if (phaseDebugIsOn && phaseDebugNotNoted) {
					Log.i("phaseDebug", "Phase took this long in nanoseconds: " + (endTime - startTime) );
					Log.i("phaseDebug", "Starting Phase: PHASE_SWAP_IN_MATRIX");
					startTime = System.nanoTime();
					phaseDebugNotNoted = false;
				}

				if (swapInMotion == false) {
					swapInMotion = true;
					swapInMatrix(myBlockMatrix);
				}
				break;

			case PHASE_TRIPLET_SEARCH:
				
				endTime = System.nanoTime();

				if (phaseDebugIsOn && phaseDebugNotNoted) {
					Log.i("phaseDebug", "Phase took this long in nanoseconds: " + (endTime - startTime) );
					Log.i("phaseDebug", "Starting Phase: PHASE_TRIPLET_SEARCH");
					startTime = System.nanoTime();
					phaseDebugNotNoted = false;
				}

				if (tripletSearchInSession == false) {
					swapInMotion = false;
					tripletSearchInSession = true;

					switch (movementDirection) {
					case MOVE_LEFT:

						finishedFirstRootSearch = tripletSearchGoingLeft(
								myBlockMatrix, activeCellRow, activeCellColumn);
						finishedSecondRootSearch = tripletSearchGoingRight(
								myBlockMatrix, activeCellRow,
								activeCellColumn + 1);

						break;

					case MOVE_RIGHT:

						finishedFirstRootSearch = tripletSearchGoingLeft(
								myBlockMatrix, activeCellRow,
								activeCellColumn - 1);
						finishedSecondRootSearch = tripletSearchGoingRight(
								myBlockMatrix, activeCellRow, activeCellColumn);

						break;

					case MOVE_UP:

						finishedFirstRootSearch = tripletSearchGoingUp(
								myBlockMatrix, activeCellRow, activeCellColumn);
						finishedSecondRootSearch = tripletSearchGoingDown(
								myBlockMatrix, activeCellRow + 1,
								activeCellColumn);

						break;

					case MOVE_DOWN:

						finishedFirstRootSearch = tripletSearchGoingUp(
								myBlockMatrix, activeCellRow - 1,
								activeCellColumn);
						finishedSecondRootSearch = tripletSearchGoingDown(
								myBlockMatrix, activeCellRow, activeCellColumn);

						break;
					}
				}

				if (finishedFirstRootSearch && finishedSecondRootSearch) {
					movementPhase = PHASE_IS_TRIPLET_FOUND;
					phaseDebugNotNoted = true;
				}

				break;

			case PHASE_IS_TRIPLET_FOUND:
				
				endTime = System.nanoTime();

				if (phaseDebugIsOn && phaseDebugNotNoted) {
					Log.i("phaseDebug", "Phase took this long in nanoseconds: " + (endTime - startTime) );
					Log.i("phaseDebug", "Starting Phase: PHASE_IS_TRIPLET_FOUND");
					startTime = System.nanoTime();
					phaseDebugNotNoted = false;
				}

				if (tripletFound) {

					if (destroyCounter == -1)
						findDeadBlocks(myBlockMatrix);
					else
						destroyBlock(myBlockMatrix);	
				}

				else {
					movementPhase = PHASE_REVERSE_MVMNT_VARS;
					phaseDebugNotNoted = true;
					moveCounter = (int) (blockWidth / movementPixels);
				}

				break;

			case PHASE_REVERSE_MVMNT_VARS:
				
				endTime = System.nanoTime();

				if (phaseDebugIsOn && phaseDebugNotNoted) {
					Log.i("phaseDebug", "Phase took this long in nanoseconds: " + (endTime - startTime) );
					Log.i("phaseDebug", "Starting Phase: PHASE_REVERSE_MVMNT_VARS");
					startTime = System.nanoTime();
					phaseDebugNotNoted = false;
				}

				if (reversingVars == false) {
					reversingVars = true;
					reverseMovementVars();
				}
				break;

			case PHASE_REVERSE_HANDLE_MOVE:
				
				endTime = System.nanoTime();

				if (phaseDebugIsOn && phaseDebugNotNoted) {
					Log.i("phaseDebug", "Phase took this long in nanoseconds: " + (endTime - startTime) );
					Log.i("phaseDebug", "Starting Phase: PHASE_REVERSE_HANDLE_MOVE");
					startTime = System.nanoTime();
					phaseDebugNotNoted = false;
				}

				if (moveCounter != -1 && numberOfFingers == 0) {
					// Log.i("READ ME", "Move Counter is " + moveCounter);
					handleMove(myBlockMatrix);
				}
				break;

			case PHASE_RESWAP_IN_MATRIX:
				
				endTime = System.nanoTime();

				if (phaseDebugIsOn && phaseDebugNotNoted) {
					Log.i("phaseDebug", "Phase took this long in nanoseconds: " + (endTime - startTime) );
					Log.i("phaseDebug", "Starting Phase: PHASE_RESWAP_IN_MATRIX");
					startTime = System.nanoTime();
					phaseDebugNotNoted = false;
					Log.i("phaseDebug", "reverseInMotion = " + reverseInMotion);
				}

				if (reverseInMotion == false) {
					reverseInMotion = true;
					swapInMatrix(myBlockMatrix);
				}
				break;

			case PHASE_DROP:
				if (phaseDebugIsOn && phaseDebugNotNoted){
					endTime = System.nanoTime();
					Log.i("phaseDebug", "Phase took this long in nanoseconds: " + (endTime - startTime) );
					Log.i("phaseDebug", "Starting Phase: PHASE_DROP");					
					startTime = System.nanoTime();
					phaseDebugNotNoted = false;
				}

				switch (phaseStep) {

				case PHASE_STEP_ONE: // Make the destroyed blocks DISAPPEAR.
					
					if (phaseStepDebugIsOn && phaseStepDebugNotNoted){
						stepStartTime = System.nanoTime();
						Log.i("phaseStepDebug", "Starting Phase Step: PHASE_STEP_ONE");
						phaseStepDebugNotNoted = false;
					}
					
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

									tmpBlockBmap = BitmapFactory.decodeResource(
											getResources(),
											R.drawable.block_gone);
									tmpBlockBmap = Bitmap.createScaledBitmap(
											tmpBlockBmap, blockWidth, blockHeight,
											true);
									myBlockMatrix[r][c].setImage(tmpBlockBmap);
								}
							}
						}
					}
					phaseStep = PHASE_STEP_TWO;
					phaseStepDebugNotNoted = true;
					break;

				
				// Run thread which FINDS which cells are TO BE DROPPED. This thread also does some swapping of block designations.
				case PHASE_STEP_TWO:
					
					if (phaseStepDebugIsOn && phaseStepDebugNotNoted){
						stepEndTime = System.nanoTime();
						Log.i("phaseStepDebug", "Phase Step took this long in nanoseconds: " + ( stepEndTime - stepStartTime));
						Log.i("phaseStepDebug", "Starting Phase Step: PHASE_STEP_TWO");
						phaseStepDebugNotNoted = false;
						stepStartTime = System.nanoTime();
					}

					// This will prevent this phase step from running again until the thread is finished.
					phaseStep = 0; 
			
					checkDrop(myBlockMatrix);

					phaseStep = PHASE_STEP_THREE;
					phaseStepDebugNotNoted = true;
					moveCounter = (int) (blockHeight / movementPixels);
					
					break;

				case PHASE_STEP_THREE: // This part does the DROP MOVEMENT
					
					if (phaseStepDebugIsOn && phaseStepDebugNotNoted){
						stepEndTime = System.nanoTime();
						Log.i("phaseStepDebug", "Phase Step took this long in nanoseconds: " + ( stepEndTime - stepStartTime));
						Log.i("phaseStepDebug", "Starting Phase Step: PHASE_STEP_THREE");
						phaseStepDebugNotNoted = false;
						stepStartTime = System.nanoTime();
					}

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
													.getY() + movementPixels);
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

						if (phaseStep != PHASE_STEP_TWO){
							movementPhase = PHASE_DONE_MOVE;
							
							if (phaseStepDebugIsOn){
								stepEndTime = System.nanoTime();
								Log.i("phaseStepDebug", "Phase Step took this long in nanoseconds: " + ( stepEndTime - stepStartTime));
							}
						}
					}

					break;
				}

				break;

			case PHASE_DONE_MOVE:
				
				endTime = System.nanoTime();

				if (phaseDebugIsOn && phaseDebugNotNoted) {
					Log.i("phaseDebug", "Phase took this long in nanoseconds: " + (endTime - startTime) );
					Log.i("phaseDebug", "Starting Phase: PHASE_DONE_MOVE");
					startTime = System.nanoTime();
					phaseDebugNotNoted = false;
				}

				doneMove(myBlockMatrix);
				movementPhase = MOVE_NULL;

				break;
			}

			ourHolder.unlockCanvasAndPost(canvas);
		}
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
	
	public void phaseHandleMove(){
		movementPhase = PHASE_HANDLE_MOVE;
		phaseDebugNotNoted = true;
	}
	

	public void setMovementVars() {
		
		// Prep variables for tracking block movement
		setMoveCounter( (int) ( getBlockWidth() / getMovementPixels() ) );
		
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
					phaseDebugNotNoted = true;
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
					phaseDebugNotNoted = true;
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
					phaseDebugNotNoted = true;
				}
			}

			// Else if the movement in Y was negative, move up
			else {

				// If the attempt to move down doesn't happen in the top cell
				if (activeCellRow > 0) {
					movementDirection = MOVE_UP;
				} else {
					movementPhase = PHASE_DONE_MOVE;
					phaseDebugNotNoted = true;
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
		phaseDebugNotNoted = true;
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
				phaseDebugNotNoted = true;
			}

			else if (movementPhase == PHASE_REVERSE_HANDLE_MOVE) {
				movementPhase = PHASE_RESWAP_IN_MATRIX;
				phaseDebugNotNoted = true;
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
		
		 if(movementPhase == PHASE_SWAP_IN_MATRIX){
			 movementPhase = PHASE_TRIPLET_SEARCH;
			 phaseDebugNotNoted = true;
		 }
		 else if (movementPhase == PHASE_RESWAP_IN_MATRIX){
			 movementPhase = PHASE_DONE_MOVE;
			 phaseDebugNotNoted = true;
		 }
			 
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
					Log.i("matchCheck",
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
					Log.i("matchCheck",
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
					Log.i("matchCheck", "Match Above and verticalTripletCount = "
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
					Log.i("matchCheck", "Match Below and verticalTripletCount = "
							+ verticalTripletCount);
				}
			}
		}

		if (verticalTripletCount == 2 || horizontalTripletCount == 2) {

			if (tripletColorDebugIsOn)
				Log.i("READ ME", "Found a triplet in the color of "
						+ currentColor);

			tripletFound = true;
			
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
					Log.i("matchCheck",
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
					Log.i("matchCheck",
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
					Log.i("matchCheck", "Match Above and verticalTripletCount = "
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
					Log.i("matchCheck", "Match Below and verticalTripletCount = "
							+ verticalTripletCount);
				}
			}
		}

		if (verticalTripletCount == 2 || horizontalTripletCount == 2) {

			if (tripletColorDebugIsOn)
				Log.i("READ ME", "Found a triplet in the color of "
						+ currentColor);

			tripletFound = true;
			
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
					Log.i("matchCheck",
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
					Log.i("matchCheck",
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
					Log.i("matchCheck", "Match Above and verticalTripletCount = "
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
					Log.i("matchCheck", "Match Below and verticalTripletCount = "
							+ verticalTripletCount);
				}
			}
		}

		if (verticalTripletCount == 2 || horizontalTripletCount == 2) {

			if (tripletColorDebugIsOn)
				Log.i("READ ME", "Found a triplet in the color of "
						+ currentColor);

			tripletFound = true;
			
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
					Log.i("matchCheck",
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
					Log.i("matchCheck",
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
					Log.i("matchCheck", "Match Above and verticalTripletCount = "
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
					Log.i("matchCheck", "Match Below and verticalTripletCount = "
							+ verticalTripletCount);
				}
			}
		}

		if (verticalTripletCount == 2 || horizontalTripletCount == 2) {

			if (tripletColorDebugIsOn)
				Log.i("READ ME", "Found a triplet in the color of "
						+ currentColor);

			tripletFound = true;
			
			if (tripletColor[0] == "none") {
				tripletColor[0] = currentColor;
			} else if (tripletColor[0] != currentColor) {
				tripletColor[1] = currentColor;
			}
		}

		return true;
	}
	
	public void findDeadBlocks(Block myBlockMatrix[][]){
		
		destroyCounter = (int) (blockWidth / movementPixels);
		shrinkCounter = (int) (blockWidth / movementPixels);

		// Rows searched in reverse because this means the information will be stored from bottom to top, 
		// making it easier to drop columns
		for (int r = matrixRows - 1; r >= 0; r--) {
			for (int c = 0; c < matrixColumns; c++) {	
			
				if (myBlockMatrix[r][c].getSearched() == true) {

					if (tripletColorDebugIsOn) {
						Log.i("READ ME", "Matrix Rows = " + matrixRows);
						Log.i("READ ME", "r = " + r);
						Log.i("READ ME", "matrixColumns = " + matrixColumns);
						Log.i("READ ME", "c = " + c);
						Log.i("READ ME", "First Color = " + tripletColor[0]);
						Log.i("READ ME", "Second Color = " + tripletColor[1]);
					}

					if (myBlockMatrix[r][c].getSearched() == true
							&& (myBlockMatrix[r][c].getColor() == tripletColor[0] 
									|| myBlockMatrix[r][c].getColor() == tripletColor[1])) {
						
						if (myBlockMatrix[r][c].getColor() == "Red")							
							destroyTheseBlocks = destroyTheseBlocks + r + c + "r";

						if (myBlockMatrix[r][c].getColor() == "Blue") 
							destroyTheseBlocks = destroyTheseBlocks + r + c + "b";
	
						if (myBlockMatrix[r][c].getColor() == "Green")
							destroyTheseBlocks = destroyTheseBlocks + r + c + "g";

						if (myBlockMatrix[r][c].getColor() == "Yellow") 
							destroyTheseBlocks = destroyTheseBlocks + r + c + "y";
					}
				}
			}
		}
	}
	
	public void destroyBlock(Block myBlockMatrix[][]) {

		Bitmap bitmap = null;
		int r, c;
		char blockColor;

		if (shrinkSoundPlayed == false) {
			shrinkSound.start();
			shrinkSoundPlayed = true;
		}

		for (int i=0; i < destroyTheseBlocks.length(); i++){
			
			r = Character.getNumericValue(destroyTheseBlocks.charAt(i));
			i++;
			c = Character.getNumericValue(destroyTheseBlocks.charAt(i));
			i++;
			
			blockColor = destroyTheseBlocks.charAt(i);
			
			if (blockColor == 'r')
				bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.block_red);
			
			if (blockColor == 'g')
				bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.block_green);
			
			if (blockColor == 'b')
				bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.block_blue);

			if (blockColor == 'y')
				bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.block_yellow);
			
			bitmap = Bitmap.createScaledBitmap(bitmap, blockWidth - (shrinkCounter), 
					blockHeight - (shrinkCounter), true);  // Resize background to fit the screen.
			myBlockMatrix[r][c].setImage(bitmap);

			if (destroyBlockDebugIsOn){
				Log.i("READ ME", "Before, X is: "
						+ myBlockMatrix[r][c].getX());	
			}
			
			myBlockMatrix[r][c]
					.setX(myBlockMatrix[r][c].getX()
							+ (float) (movementPixels / 2));
			myBlockMatrix[r][c]
					.setY(myBlockMatrix[r][c].getY()
							+ (float) (movementPixels / 2));

			if (destroyBlockDebugIsOn){
				Log.i("READ ME", "After, X is: "
						+ myBlockMatrix[r][c].getX());
			}
		}

		if (destroyCounter == FINISHED_MOVING) {

			// Turn the destroyed blocks into puffs of smoke.
			for (int mR = 0; mR < matrixRows; mR++) {
				for (int mC = 0; mC < matrixColumns; mC++) {

					if (myBlockMatrix[mR][mC].getSearched() == true
							&& (myBlockMatrix[mR][mC].getColor() == tripletColor[0] || myBlockMatrix[mR][mC]
									.getColor() == tripletColor[1])) {

						if (myBlockMatrix[mR][mC].getSearched() == true) {

							bitmap = BitmapFactory.decodeResource(
									getResources(), R.drawable.block_destroyed);
							bitmap = Bitmap.createScaledBitmap(bitmap,
									blockWidth, blockHeight, true);
							myBlockMatrix[mR][mC].setImage(bitmap);
							myBlockMatrix[mR][mC]
									.setX(myMatrixCoordinates[mR][mC].x);
							myBlockMatrix[mR][mC]
									.setY(myMatrixCoordinates[mR][mC].y);
						}
					}
				}
			}
			moveCounter = (int) (blockWidth / movementPixels);
			movementPhase = PHASE_DROP;
			phaseDebugNotNoted = true;
			phaseStep = PHASE_STEP_ONE;
		}

		shrinkCounter = shrinkCounter + (int) movementPixels;
		destroyCounter--;
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
	
	
	public void doneMove(Block myBlockMatrix[][]) {
		downX = downY = upX = upY = 0;
		moveCounter = -1;

		destroyCounter = -1;
		destroyTheseBlocks = "";
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
		phaseDebugNotNoted = true;

		reversingVars = false;
		reverseInMotion = false;

		tripletColor[0] = "none";
		tripletColor[1] = "none";

		phaseStep = PHASE_STEP_ONE;

		tripletSearchInSession = false;
		finishedFirstRootSearch = false;
		finishedSecondRootSearch = false;
		
		phaseStepDebugNotNoted = true;

		for (int i = 0; i < matrixRows; i++) {
			for (int j = 0; j < matrixColumns; j++) {
				myBlockMatrix[i][j].setSearched(false);
			}
		}

		for (int i = 0; i < matrixColumns; i++)
			dropColumns[i] = NO_DROP;
	}
	

	public void setNumberOfFingers(int numberOfFingers) {
		this.numberOfFingers = numberOfFingers;
	}

	public void reduceNumberOfFingers() {
		this.numberOfFingers--;
	}

	public void setUpX(float upX) {
		this.upX = upX;
	}

	public void setDownX(float downX) {
		this.downX = downX;
	}

	public void setUpY(float upY) {
		this.upY = upY;
	}

	public void setDownY(float downY) {
		this.downY = downY;
	}

	boolean isFreeToMove() {

		return freeToMove;
	}

	void setMatrixColumns(int columns) {
		matrixColumns = columns;
	}

	int getMatrixColumns() {
		return matrixColumns;
	}

	void setMatrixRows(int rows) {
		matrixRows = rows;
	}

	public int getBlockWidth() {
		return blockWidth;
	}

	public void setBlockWidth(int blockWidth) {
		this.blockWidth = blockWidth;
	}

	public int getBlockHeight() {
		return blockHeight;
	}

	public void setBlockHeight(int blockHeight) {
		this.blockHeight = blockHeight;
	}

	void setMoveCounter(int moveCounter){
		this.moveCounter = moveCounter;
	}
	void setMovementPixels(float mPixels) {
		movementPixels = mPixels;
	}

	float getMovementPixels() {
		return movementPixels;
	}
	
}

