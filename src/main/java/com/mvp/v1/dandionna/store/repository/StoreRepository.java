package com.mvp.v1.dandionna.store.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mvp.v1.dandionna.store.entity.Store;

public interface StoreRepository extends JpaRepository<Store, UUID> {
    Optional<Store> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);
    Optional<Store> findByOwnerUserId(UUID ownerUserId);

    @Query(value = """
        SELECT s.id, s.name, s.open_time, s.close_time, s.image_key,
               ST_DistanceSphere(s.geom, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)) AS distance_m
        FROM stores s
        WHERE EXISTS (
            SELECT 1 FROM no_show_posts p
            WHERE p.store_id = s.id
              AND p.status = 'open'
              AND p.expire_at > now()
        )
        ORDER BY distance_m ASC
        """,
        countQuery = """
        SELECT COUNT(*) FROM stores s
        WHERE EXISTS (
            SELECT 1 FROM no_show_posts p
            WHERE p.store_id = s.id
              AND p.status = 'open'
              AND p.expire_at > now()
        )
        """,
        nativeQuery = true)
    Page<Object[]> findNearbyStores(@Param("lat") double lat, @Param("lon") double lon, Pageable pageable);
}
