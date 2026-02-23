package com.mvp.v1.dandionna.notification.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mvp.v1.dandionna.notification.dto.NotificationHistoryResponse;
import com.mvp.v1.dandionna.notification.entity.NotificationTarget;

public interface NotificationTargetRepository extends JpaRepository<NotificationTarget, Long> {

	@Query("""
		select new com.mvp.v1.dandionna.notification.dto.NotificationHistoryResponse(
			n.id, n.title, n.body, n.category, nt.status, nt.createdAt
		)
		from NotificationTarget nt
		join Notification n on n.id = nt.notificationId
		where nt.userId = :userId
		order by nt.createdAt desc
		""")
	Page<NotificationHistoryResponse> findHistoryByUserId(@Param("userId") UUID userId, Pageable pageable);

	@Modifying
	@Query("""
		update NotificationTarget nt
		set nt.status = 'SENT',
		    nt.attemptCount = nt.attemptCount + 1,
		    nt.lastErrorCode = null,
		    nt.lastErrorMessage = null,
		    nt.nextRetryAt = null,
		    nt.messageId = :messageId,
		    nt.updatedAt = CURRENT_TIMESTAMP
		where nt.id = :id
		""")
	int markSuccess(@Param("id") Long id, @Param("messageId") String messageId);

	@Modifying
	@Query("""
		update NotificationTarget nt
		set nt.status = :status,
		    nt.attemptCount = :attempt,
		    nt.lastErrorCode = :errorCode,
		    nt.lastErrorMessage = :errorMessage,
		    nt.nextRetryAt = :nextRetryAt,
		    nt.updatedAt = CURRENT_TIMESTAMP
		where nt.id = :id
		""")
	int markFailure(@Param("id") Long id,
		@Param("status") String status,
		@Param("attempt") int attempt,
		@Param("errorCode") String errorCode,
		@Param("errorMessage") String errorMessage,
		@Param("nextRetryAt") OffsetDateTime nextRetryAt);
}
