package com.mvp.v1.dandionna.notification.worker;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mvp.v1.dandionna.fcm.service.FcmNotificationService;
import com.mvp.v1.dandionna.notification.entity.NotificationTarget;
import com.mvp.v1.dandionna.notification.repository.NotificationTargetRepository;

/**
 * Redis Stream 기반 알림 전송 워커 스켈레톤.
 * - Stream key: notification:queue
 * - Consumer group/offset 관리, FCM 전송/DB 상태 업데이트는 추후 확장
 */
@Component
public class NotificationDispatchWorker {

	private static final Logger log = LoggerFactory.getLogger(NotificationDispatchWorker.class);
	private static final String STREAM_KEY = "notification:queue";
	private static final String GROUP = "notification-workers";
	private static final String CONSUMER = "worker-1";

	private final StringRedisTemplate redisTemplate;
	private final FcmNotificationService fcmNotificationService;
	private final NotificationTargetRepository notificationTargetRepository;

	public NotificationDispatchWorker(StringRedisTemplate redisTemplate,
		FcmNotificationService fcmNotificationService,
		NotificationTargetRepository notificationTargetRepository) {
		this.redisTemplate = redisTemplate;
		this.fcmNotificationService = fcmNotificationService;
		this.notificationTargetRepository = notificationTargetRepository;
		ensureGroup();
	}

	private void ensureGroup() {
		try {
			redisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP);
		} catch (Exception ignored) {
			// group already exists or stream absent; will be created on first add
		}
	}

	/**
	 * 스켈레톤: 호출 시 한 번 읽어 처리. (스케줄러/배치에서 호출 예정)
	 */
	@Transactional
	public void processOnce() {
		// NOTE: 현재 비동기 전송 워커는 스켈레톤 상태입니다.
		// - DB 상태 업데이트/재시도 로직은 구현되어 있지만,
		// - 프로듀서가 Stream에 targetId 등을 enqueue하지 않은 상태라 실제 호출되지 않습니다.
		// - 향후 프로듀서 연결 후 enable 하면 됩니다.
		List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
			org.springframework.data.redis.connection.stream.Consumer.from(GROUP, CONSUMER),
			org.springframework.data.redis.connection.stream.StreamReadOptions.empty().count(10).block(Duration.ofSeconds(1)),
			StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
		);
		if (records == null || records.isEmpty()) {
			return;
		}

		for (MapRecord<String, Object, Object> record : records) {
			try {
				Map<Object, Object> values = record.getValue();
				DispatchPayload payload = DispatchPayload.from(values);
				if (payload == null) {
					continue;
				}
				NotificationTarget target = payload.targetId() != null
					? notificationTargetRepository.findById(payload.targetId()).orElse(null)
					: null;
				UUID targetUserId = target != null ? target.getUserId() : payload.userId();
				int attemptFromDb = target != null ? target.getAttemptCount() : payload.attempt();

				log.debug("Dispatching notification targetId={} payload={}", payload.targetId(), values);
				boolean sent = fcmNotificationService.sendToUser(targetUserId, payload.title(), payload.body(), payload.data());
				if (sent) {
					if (target != null) {
						notificationTargetRepository.markSuccess(payload.targetId(), null);
					}
					continue;
				}
				int nextAttempt = attemptFromDb + 1;
				int backoffSec = backoffSeconds(nextAttempt);
				String status = nextAttempt > 3 ? "FAILED" : "QUEUED";
				OffsetDateTime nextRetryAt = status.equals("FAILED") ? null : OffsetDateTime.now().plusSeconds(backoffSec);
				if (target != null) {
					notificationTargetRepository.markFailure(
						payload.targetId(),
						status,
						nextAttempt,
						"FCM_FAILED",
						"FCM send failed",
						nextRetryAt
					);
				}
				if ("QUEUED".equals(status)) {
					requeue(payload, nextAttempt, nextRetryAt);
				}
			} catch (Exception e) {
				log.warn("Failed to process notification record id={}: {}", record.getId(), e.getMessage());
			} finally {
				redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, record.getId());
			}
		}
	}

	private int backoffSeconds(int attempt) {
		return switch (attempt) {
			case 1 -> 5;
			case 2 -> 10;
			default -> 15;
		};
	}

	private void requeue(DispatchPayload payload, int attempt, OffsetDateTime nextRetryAt) {
		Map<String, String> map = new java.util.HashMap<>();
		if (payload.targetId() != null) {
			map.put("targetId", payload.targetId().toString());
		}
		map.put("userId", payload.userId().toString());
		map.put("title", payload.title());
		map.put("body", payload.body());
		map.put("attempt", String.valueOf(attempt));
		if (nextRetryAt != null) {
			map.put("nextRetryAt", nextRetryAt.toString());
		}
		if (payload.data() != null) {
			payload.data().forEach((k, v) -> map.put("data." + k, v));
		}
		redisTemplate.opsForStream().add(STREAM_KEY, map);
	}

	private record DispatchPayload(Long targetId, UUID userId, String title, String body, Map<String, String> data, int attempt) {
		static DispatchPayload from(Map<Object, Object> map) {
			if (map.get("userId") == null) {
				return null;
			}
			Long targetId = map.get("targetId") != null ? Long.valueOf(String.valueOf(map.get("targetId"))) : null;
			UUID userId = UUID.fromString(String.valueOf(map.get("userId")));
			String title = String.valueOf(map.getOrDefault("title", ""));
			String body = String.valueOf(map.getOrDefault("body", ""));
			int attempt = 0;
			if (map.containsKey("attempt")) {
				try {
					attempt = Integer.parseInt(String.valueOf(map.get("attempt")));
				} catch (NumberFormatException ignored) {
					attempt = 0;
				}
			}
			Map<String, String> data = new java.util.HashMap<>();
			for (Map.Entry<Object, Object> entry : map.entrySet()) {
				String key = String.valueOf(entry.getKey());
				if (key.equals("userId") || key.equals("title") || key.equals("body") || key.equals("attempt")) {
					continue;
				}
				if (key.startsWith("data.")) {
					data.put(key.substring(5), String.valueOf(entry.getValue()));
				}
			}
			return new DispatchPayload(targetId, userId, title, body, data.isEmpty() ? null : data, attempt);
		}
	}
}
