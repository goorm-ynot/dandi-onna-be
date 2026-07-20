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

import io.micrometer.core.instrument.Counter;

/**
 * Redis Stream 기반 알림 전송 워커.
 * - Stream key: notification:queue
 * - 최대 3회 재시도 후 DLQ(notification:dlq)로 이동
 * - Consumer group 기반 처리, FCM 전송 및 DB 상태 업데이트
 */
@Component
public class NotificationDispatchWorker {

	private static final Logger log = LoggerFactory.getLogger(NotificationDispatchWorker.class);
	private static final String STREAM_KEY = "notification:queue";
	private static final String DLQ_STREAM_KEY = "notification:dlq";
	private static final String GROUP = "notification-workers";
	private static final String CONSUMER = "worker-1";
	private static final int MAX_ATTEMPTS = 3;

	private final StringRedisTemplate redisTemplate;
	private final FcmNotificationService fcmNotificationService;
	private final NotificationTargetRepository notificationTargetRepository;
	private final Counter notificationSentCounter;
	private final Counter notificationFailedCounter;

	public NotificationDispatchWorker(StringRedisTemplate redisTemplate,
		FcmNotificationService fcmNotificationService,
		NotificationTargetRepository notificationTargetRepository,
		Counter notificationSentCounter,
		Counter notificationFailedCounter) {
		this.redisTemplate = redisTemplate;
		this.fcmNotificationService = fcmNotificationService;
		this.notificationTargetRepository = notificationTargetRepository;
		this.notificationSentCounter = notificationSentCounter;
		this.notificationFailedCounter = notificationFailedCounter;
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
					notificationSentCounter.increment();
					continue;
				}
				int nextAttempt = attemptFromDb + 1;
				if (nextAttempt > MAX_ATTEMPTS) {
					// 최대 재시도 초과 → DLQ로 이동
					moveToDlq(payload, nextAttempt, "FCM_FAILED", "FCM send failed after " + MAX_ATTEMPTS + " attempts");
					if (target != null) {
						notificationTargetRepository.markFailure(
							payload.targetId(), "FAILED", nextAttempt,
							"FCM_FAILED", "FCM send failed after " + MAX_ATTEMPTS + " attempts", null
						);
					}
					notificationFailedCounter.increment();
					log.warn("알림 DLQ 이동: targetId={}, userId={}, attempt={}", payload.targetId(), payload.userId(), nextAttempt);
				} else {
					// 재시도 예약
					int backoffSec = backoffSeconds(nextAttempt);
					OffsetDateTime nextRetryAt = OffsetDateTime.now().plusSeconds(backoffSec);
					if (target != null) {
						notificationTargetRepository.markFailure(
							payload.targetId(), "QUEUED", nextAttempt,
							"FCM_FAILED", "FCM send failed", nextRetryAt
						);
					}
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

	/**
	 * DLQ 메시지를 메인 큐로 재투입한다 (attempt 초기화).
	 * @return 재투입된 메시지 수
	 */
	public int replayDlq(int maxCount) {
		List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
			StreamOffset.create(DLQ_STREAM_KEY, ReadOffset.from("0-0"))
		);
		if (records == null || records.isEmpty()) {
			return 0;
		}
		int replayed = 0;
		for (MapRecord<String, Object, Object> record : records) {
			if (replayed >= maxCount) {
				break;
			}
			DispatchPayload payload = DispatchPayload.from(record.getValue());
			if (payload != null) {
				requeue(payload, 0, null);
				if (payload.targetId() != null) {
					notificationTargetRepository.markFailure(
						payload.targetId(), "QUEUED", 0,
						null, "DLQ 재투입", OffsetDateTime.now()
					);
				}
			}
			redisTemplate.opsForStream().delete(DLQ_STREAM_KEY, record.getId());
			replayed++;
		}
		log.info("DLQ 재처리 완료: {}건", replayed);
		return replayed;
	}

	/**
	 * DLQ 메시지 수를 반환한다.
	 */
	public long getDlqSize() {
		Long size = redisTemplate.opsForStream().size(DLQ_STREAM_KEY);
		return size != null ? size : 0;
	}

	private void moveToDlq(DispatchPayload payload, int attempt, String errorCode, String errorMessage) {
		Map<String, String> map = new java.util.HashMap<>();
		if (payload.targetId() != null) {
			map.put("targetId", payload.targetId().toString());
		}
		map.put("userId", payload.userId().toString());
		map.put("title", payload.title());
		map.put("body", payload.body());
		map.put("attempt", String.valueOf(attempt));
		map.put("errorCode", errorCode);
		map.put("errorMessage", errorMessage);
		map.put("failedAt", OffsetDateTime.now().toString());
		if (payload.data() != null) {
			payload.data().forEach((k, v) -> map.put("data." + k, v));
		}
		redisTemplate.opsForStream().add(DLQ_STREAM_KEY, map);
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
