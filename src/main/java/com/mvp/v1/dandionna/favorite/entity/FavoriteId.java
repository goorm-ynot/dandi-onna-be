package com.mvp.v1.dandionna.favorite.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
public class FavoriteId implements Serializable {

	@Column(name = "consumer_user_id", nullable = false)
	private UUID consumerUserId;

	@Column(name = "store_id", nullable = false)
	private UUID storeId;

	public FavoriteId(UUID consumerUserId, UUID storeId) {
		this.consumerUserId = consumerUserId;
		this.storeId = storeId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FavoriteId favoriteId = (FavoriteId) o;
		return Objects.equals(consumerUserId, favoriteId.consumerUserId)
			&& Objects.equals(storeId, favoriteId.storeId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(consumerUserId, storeId);
	}
}
