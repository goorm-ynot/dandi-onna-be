package com.mvp.v1.dandionna.menu.entity;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MenuStatus {
	on_sale,
	sold_out;

	@JsonCreator
	public static MenuStatus from(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return MenuStatus.valueOf(value.trim().toLowerCase(Locale.ROOT));
	}

	@JsonValue
	public String toJson() {
		return name().toUpperCase(Locale.ROOT);
	}

	public boolean isOnSale() {
		return this == on_sale;
	}
}
