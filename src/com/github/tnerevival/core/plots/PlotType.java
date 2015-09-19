package com.github.tnerevival.core.plots;

public enum PlotType {
	BANK(1, "Bank"),
	PERSONAL(2, "Personal"),
	TRADE(3, "Trade");
	
	private int id;
	private String type;
	
	PlotType(int id, String type) {
		this.id = id;
		this.type = type;
	}
	
	public int getID() {
		return this.id;
	}
	
	public String getType() {
		return this.type;
	}
	
	public String getType(int id) {
		for(PlotType type : values()) {
			if(type.getID() == id) {
				return type.getType();
			}
		}
		return null;
	}
}