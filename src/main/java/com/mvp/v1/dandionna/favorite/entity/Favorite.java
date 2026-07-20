package com.mvp.v1.dandionna.favorite.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "favorites")
@Getter
@NoArgsConstructor
public class Favorite {

	@EmbeddedId
	private FavoriteId id;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	private Favorite(FavoriteId id) {
		this.id = id;
	}

	public static Favorite create(UUID consumerId, UUID storeId) {
		return new Favorite(new FavoriteId(consumerId, storeId));
	}
}
