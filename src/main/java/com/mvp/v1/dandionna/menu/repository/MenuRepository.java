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

	List<Menu> findByStoreIdOrderByCreatedAtDescIdDesc(UUID storeId);

	List<Menu> findByStoreIdAndTypeOrderByCreatedAtDescIdDesc(UUID storeId, MenuType type);

	List<Menu> findByIdIn(Collection<UUID> ids);

	List<Menu> findByStoreIdAndIdIn(UUID storeId, Collection<UUID> ids);

	Optional<Menu> findByIdAndStoreId(UUID id, UUID storeId);

	@Query("""
		select m
		from Menu m
		where m.storeId = :storeId
		  and (lower(m.name) like lower(concat('%', :keyword, '%'))
		    or lower(coalesce(m.description, '')) like lower(concat('%', :keyword, '%')))
		order by m.createdAt desc, m.id desc
		""")
	List<Menu> searchByKeyword(
		@Param("storeId") UUID storeId,
		@Param("keyword") String keyword
	);

	@Query("""
		select m
		from Menu m
		where m.storeId = :storeId
		  and (lower(m.name) like lower(concat('%', :keyword, '%'))
		    or lower(coalesce(m.description, '')) like lower(concat('%', :keyword, '%')))
		  and m.type = :type
		order by m.createdAt desc, m.id desc
		""")
	List<Menu> searchByKeywordAndType(
		@Param("storeId") UUID storeId,
		@Param("keyword") String keyword,
		@Param("type") MenuType type
	);

	default List<Menu> search(UUID storeId, String keyword, MenuType type) {
		if ((keyword == null || keyword.isBlank()) && type == null) {
			return findByStoreIdOrderByCreatedAtDescIdDesc(storeId);
		}
		if (keyword == null || keyword.isBlank()) {
			return findByStoreIdAndTypeOrderByCreatedAtDescIdDesc(storeId, type);
		}
		if (type == null) {
			return searchByKeyword(storeId, keyword);
		}
		return searchByKeywordAndType(storeId, keyword, type);
	}
}
