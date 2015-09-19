package com.github.tnerevival.utils;

public class Vector2D {
	
	public static final Vector2D ZERO = new Vector2D(0, 0);
	
	int x;
	int z;
	
	public Vector2D(int x, int z) {
		this.x = x;
		this.z = z;
	}
	
	public int getX() {
		return this.x;
	}
	
	public int getZ() {
		return this.z;
	}
}