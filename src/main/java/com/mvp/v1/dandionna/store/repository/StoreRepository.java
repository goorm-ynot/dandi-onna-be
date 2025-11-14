package com.mvp.v1.dandionna.store.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mvp.v1.dandionna.store.entity.Store;

public interface StoreRepository extends JpaRepository<Store, UUID> {
    Optional<Store> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);
    Optional<Store> findByOwnerUserId(UUID ownerUserId);
}
