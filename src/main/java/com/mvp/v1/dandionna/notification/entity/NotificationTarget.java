package com.mvp.v1.dandionna.notification.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_targets")
@Getter
@NoArgsConstructor
public class NotificationTarget {

	@Id
	private Long id;

	@Column(name = "notification_id", nullable = false)
	private UUID notificationId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "status", nullable = false)
	private String status;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "last_error_code")
	private String lastErrorCode;

	@Column(name = "last_error_message")
	private String lastErrorMessage;

	@Column(name = "next_retry_at")
	private OffsetDateTime nextRetryAt;

	@Column(name = "channel")
	private String channel;

	@Column(name = "message_id")
	private String messageId;

	@Column(name = "created_at")
	private OffsetDateTime createdAt;

	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;
}
