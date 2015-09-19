package com.github.tnerevival.core.plots;

import java.util.UUID;

import com.github.tnerevival.utils.Vector2D;

public class Plot {
	
	UUID id;
	UUID owner;
	String name;
	int type;
	Vector2D min;
	Vector2D max;
	
	public Plot(UUID owner) {
		this(owner, Vector2D.ZERO, Vector2D.ZERO);
	}
	
	public Plot(UUID owner, Vector2D min, Vector2D max) {
		this.owner = owner;
		this.min = min;
		this.max = max;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getOwner() {
		return owner;
	}

	public void setOwner(UUID owner) {
		this.owner = owner;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Vector2D getMin() {
		return min;
	}

	public void setMin(Vector2D min) {
		this.min = min;
	}

	public Vector2D getMax() {
		return max;
	}

	public void setMax(Vector2D max) {
		this.max = max;
	}
}