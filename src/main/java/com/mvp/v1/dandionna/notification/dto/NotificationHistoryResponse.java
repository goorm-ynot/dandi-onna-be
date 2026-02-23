package com.mvp.v1.dandionna.notification.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationHistoryResponse(
	UUID notificationId,
	String title,
	String body,
	String category,
	String status,
	OffsetDateTime createdAt
) {
}
