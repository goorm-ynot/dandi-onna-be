package com.mvp.v1.dandionna.notification.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvp.v1.dandionna.notification.producer.NotificationProducer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationEnqueueService {

	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;
	private final NotificationProducer notificationProducer;

	@Transactional
	public void enqueue(UUID userId, String title, String body, Map<String, String> data) {
		UUID notificationId = insertNotification(title, body, data);
		Long targetId = insertTarget(notificationId, userId);
		notificationProducer.enqueueWithTarget(targetId, userId, title, body, data);
	}

	private UUID insertNotification(String title, String body, Map<String, String> data) {
		String json = "{}";
		if (data != null && !data.isEmpty()) {
			try {
				json = objectMapper.writeValueAsString(data);
			} catch (JsonProcessingException ignored) {
			}
		}
		return jdbcTemplate.queryForObject(
			"insert into notifications (title, body, data) values (:title, :body, cast(:data as jsonb)) returning id",
			new MapSqlParameterSource()
				.addValue("title", title)
				.addValue("body", body)
				.addValue("data", json),
			UUID.class
		);
	}

	private Long insertTarget(UUID notificationId, UUID userId) {
		return jdbcTemplate.queryForObject(
			"insert into notification_targets (notification_id, user_id, status, attempt_count) values (:nid, :uid, 'QUEUED', 0) returning id",
			new MapSqlParameterSource()
				.addValue("nid", notificationId)
				.addValue("uid", userId),
			Long.class
		);
	}
}
