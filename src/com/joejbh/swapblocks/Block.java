package com.joejbh.swapblocks;

import android.graphics.Bitmap;

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
