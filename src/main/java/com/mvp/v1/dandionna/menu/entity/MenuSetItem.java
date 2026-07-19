package com.mvp.v1.dandionna.menu.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "menu_set_items")
public class MenuSetItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "set_menu_id", nullable = false)
	private UUID setMenuId;

	@Column(name = "component_menu_id", nullable = false)
	private UUID componentMenuId;

	@Column(name = "quantity", nullable = false)
	private int quantity;

	protected MenuSetItem() {
	}

	private MenuSetItem(UUID setMenuId, UUID componentMenuId, int quantity) {
		this.setMenuId = setMenuId;
		this.componentMenuId = componentMenuId;
		this.quantity = quantity;
	}

	public static MenuSetItem create(UUID setMenuId, UUID componentMenuId, int quantity) {
		return new MenuSetItem(setMenuId, componentMenuId, quantity);
	}

	public Long getId() {
		return id;
	}

	public UUID getSetMenuId() {
		return setMenuId;
	}

	public UUID getComponentMenuId() {
		return componentMenuId;
	}

	public int getQuantity() {
		return quantity;
	}
}
