package com.mvp.v1.dandionna.noshow_post.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mvp.v1.dandionna.noshow_post.entity.NoShowPostScheduleItem;

public interface NoShowPostScheduleItemRepository extends JpaRepository<NoShowPostScheduleItem, Long> {

	List<NoShowPostScheduleItem> findBySchedule_IdOrderByIdAsc(UUID scheduleId);

	@Query("""
		select i.schedule.id, count(i.id)
		from NoShowPostScheduleItem i
		where i.schedule.id in :scheduleIds
		group by i.schedule.id
		""")
	List<Object[]> countByScheduleIds(@Param("scheduleIds") Collection<UUID> scheduleIds);
}

