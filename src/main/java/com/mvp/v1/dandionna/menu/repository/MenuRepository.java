package com.mvp.v1.dandionna.menu.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mvp.v1.dandionna.menu.entity.Menu;

public interface MenuRepository extends JpaRepository<Menu, UUID> {

	boolean existsByIdAndStoreId(UUID id, UUID storeId);

	List<Menu> findByStoreIdAndIdIn(UUID storeId, Collection<UUID> ids);
}
