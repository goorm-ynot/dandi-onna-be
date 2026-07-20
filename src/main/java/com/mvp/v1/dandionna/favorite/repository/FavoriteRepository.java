package com.mvp.v1.dandionna.favorite.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.mvp.v1.dandionna.favorite.entity.Favorite;
import com.mvp.v1.dandionna.favorite.entity.FavoriteId;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {

	boolean existsById(FavoriteId id);

	void deleteById(FavoriteId id);

	@Query("select f.id.consumerUserId from Favorite f where f.id.storeId = :storeId")
	List<UUID> findConsumerIdsByStoreId(UUID storeId);

	boolean existsByIdConsumerUserIdAndIdStoreId(UUID consumerUserId, UUID storeId);
}
