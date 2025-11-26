package com.mvp.v1.dandionna.notification.producer;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 알림 전송을 Redis Stream에 enqueue하는 프로듀서.
 * - 워커가 비동기로 읽어 FCM 전송/재시도를 처리한다.
 * - targetId(notification_targets.id)를 함께 넣으면 DB 상태 업데이트·재시도가 가능하다.
 */
@Component
public class NotificationProducer {

	private static final String STREAM_KEY = "notification:queue";
	private final StringRedisTemplate redisTemplate;

	public NotificationProducer(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void enqueue(UUID userId, String title, String body, Map<String, String> data) {
		enqueueWithTarget(null, userId, title, body, data);
	}

	public void enqueueWithTarget(Long targetId, UUID userId, String title, String body, Map<String, String> data) {
		var map = new java.util.HashMap<String, String>();
		if (targetId != null) {
			map.put("targetId", targetId.toString());
		}
		map.put("userId", userId.toString());
		map.put("title", title);
		map.put("body", body);
		map.put("attempt", "0");
		if (data != null) {
			data.forEach((k, v) -> map.put("data." + k, v));
		}
		redisTemplate.opsForStream().add(STREAM_KEY, map);
	}
}
