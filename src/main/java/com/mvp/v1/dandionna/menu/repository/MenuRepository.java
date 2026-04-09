package com.mvp.v1.dandionna.menu.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mvp.v1.dandionna.menu.entity.Menu;
import com.mvp.v1.dandionna.menu.entity.MenuType;

public interface MenuRepository extends JpaRepository<Menu, UUID> {

	boolean existsByIdAndStoreId(UUID id, UUID storeId);

	List<Menu> findByIdIn(Collection<UUID> ids);

	List<Menu> findByStoreIdAndIdIn(UUID storeId, Collection<UUID> ids);

	Optional<Menu> findByIdAndStoreId(UUID id, UUID storeId);

	@Query("""
		select m
		from Menu m
		where m.storeId = :storeId
		  and (:keyword is null
		    or lower(m.name) like lower(concat('%', :keyword, '%'))
		    or lower(coalesce(m.description, '')) like lower(concat('%', :keyword, '%')))
		  and (:type is null or m.type = :type)
		order by m.createdAt desc, m.id desc
		""")
	List<Menu> search(
		@Param("storeId") UUID storeId,
		@Param("keyword") String keyword,
		@Param("type") MenuType type
	);
}
