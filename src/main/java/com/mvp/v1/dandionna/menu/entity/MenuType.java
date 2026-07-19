package com.mvp.v1.dandionna.menu.entity;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MenuType {
	single,
	set;

	@JsonCreator
	public static MenuType from(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return MenuType.valueOf(value.trim().toLowerCase(Locale.ROOT));
	}

	@JsonValue
	public String toJson() {
		return name().toUpperCase(Locale.ROOT);
	}

	public boolean isSet() {
		return this == set;
	}

	public boolean isSingle() {
		return this == single;
	}
}
