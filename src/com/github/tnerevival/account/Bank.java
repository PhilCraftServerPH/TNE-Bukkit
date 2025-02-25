package com.github.tnerevival.account;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.github.tnerevival.serializable.SerializableItemStack;
import com.github.tnerevival.utils.MISCUtils;

public class Bank implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	List<SerializableItemStack> items = new ArrayList<SerializableItemStack>();
	UUID id;
	@Deprecated
	String owner;
	String pin;
	Integer size;
	Double gold;
	
	@Deprecated
	public Bank(String owner, Integer size) {
		this.owner = owner;
		this.pin = "none";
		this.size = size;
		this.gold = 0.0;
	}
	
	@Deprecated
	public Bank(String owner, Integer size, Double gold) {
		this.owner = owner;
		this.pin = "none";
		this.size = size;
		this.gold = gold;
	}
	
	public Bank(UUID uid, Integer size) {
		this.id = uid;
		this.pin = "none";
		this.size = size;
		this.gold = 0.0;
	}
	
	public Bank(UUID uid, Integer size, Double gold) {
		this.id = uid;
		this.pin = "none";
		this.size = size;
		this.gold = gold;
	}

	/**
	 * @return the items
	 */
	public List<SerializableItemStack> getItems() {
		return items;
	}

	/**
	 * @param items the items to set
	 */
	public void setItems(List<SerializableItemStack> items) {
		this.items = items;
	}

	/**
	 * @return the owner
	 */
	@Deprecated
	public String getOwner() {
		return owner;
	}

	/**
	 * @param owner the owner to set
	 */
	@Deprecated
	public void setOwner(String owner) {
		this.owner = owner;
	}

	/**
	 * @return the pin
	 */
	public String getPin() {
		return pin;
	}

	/**
	 * @param pin the pin to set
	 */
	public void setPin(String pin) {
		this.pin = pin;
	}

	/**
	 * @return the size
	 */
	public Integer getSize() {
		return size;
	}

	/**
	 * @param size the size to set
	 */
	public void setSize(Integer size) {
		this.size = size;
	}

	/**
	 * @return the gold
	 */
	public Double getGold() {
		return gold;
	}

	/**
	 * @param gold the gold to set
	 */
	public void setGold(Double gold) {
		this.gold = gold;
	}
	
	public String itemsToString() {
		if(!items.isEmpty()) {
			String toReturn = "";
			
			int count = 0;
			for(SerializableItemStack item : items) {
				if(item != null) {
					if(count != 0) {
						toReturn += "*";
					}
					toReturn += MISCUtils.itemstackToString(item);
					count++;
				}
			}
			return toReturn;
		}
		return "TNENOSTRINGVALUE";
	}
	
	public String toString() {
		return owner + ":" + pin + ":" + size + ":" + gold + ":" + itemsToString();
	}
}