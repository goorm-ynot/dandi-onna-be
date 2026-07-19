package com.mvp.v1.dandionna.menu.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mvp.v1.dandionna.menu.entity.MenuSetItem;

public interface MenuSetItemRepository extends JpaRepository<MenuSetItem, Long> {

	List<MenuSetItem> findBySetMenuIdOrderByIdAsc(UUID setMenuId);

	List<MenuSetItem> findBySetMenuIdIn(Collection<UUID> setMenuIds);

	List<MenuSetItem> findByComponentMenuIdIn(Collection<UUID> componentMenuIds);

	boolean existsByComponentMenuId(UUID componentMenuId);

	void deleteBySetMenuId(UUID setMenuId);
}
