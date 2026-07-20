package com.mvp.v1.dandionna.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvp.v1.dandionna.notification.producer.NotificationProducer;

@ExtendWith(MockitoExtension.class)
class NotificationEnqueueServiceTest {

	@Mock
	private NamedParameterJdbcTemplate jdbcTemplate;
	@Mock
	private ObjectMapper objectMapper;
	@Mock
	private NotificationProducer producer;

	@InjectMocks
	private NotificationEnqueueService service;

	private UUID notificationId;
	private Long targetId;

	@BeforeEach
	void setup() {
		notificationId = UUID.randomUUID();
		targetId = 123L;
	}

	@Test
	void enqueue_savesNotificationAndTarget_thenPublishStream() throws JsonProcessingException {
		UUID userId = UUID.randomUUID();
		Map<String, String> data = Map.of("deeplink", "/seller/order");

		when(objectMapper.writeValueAsString(data)).thenReturn("{\"deeplink\":\"/seller/order\"}");
		when(jdbcTemplate.queryForObject(eq("insert into notifications (title, body, data) values (:title, :body, cast(:data as jsonb)) returning id"),
			any(MapSqlParameterSource.class), eq(UUID.class))).thenReturn(notificationId);
		when(jdbcTemplate.queryForObject(eq("insert into notification_targets (notification_id, user_id, status, attempt_count) values (:nid, :uid, 'QUEUED', 0) returning id"),
			any(MapSqlParameterSource.class), eq(Long.class))).thenReturn(targetId);

		service.enqueue(userId, "title", "body", data);

		verify(jdbcTemplate).queryForObject(eq("insert into notifications (title, body, data) values (:title, :body, cast(:data as jsonb)) returning id"),
			any(MapSqlParameterSource.class), eq(UUID.class));
		verify(jdbcTemplate).queryForObject(eq("insert into notification_targets (notification_id, user_id, status, attempt_count) values (:nid, :uid, 'QUEUED', 0) returning id"),
			any(MapSqlParameterSource.class), eq(Long.class));
		verify(producer).enqueueWithTarget(targetId, userId, "title", "body", data);
	}
}
