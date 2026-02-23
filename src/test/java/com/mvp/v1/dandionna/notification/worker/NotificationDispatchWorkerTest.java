package com.mvp.v1.dandionna.notification.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.mvp.v1.dandionna.fcm.service.FcmNotificationService;
import com.mvp.v1.dandionna.notification.repository.NotificationTargetRepository;

import io.micrometer.core.instrument.Counter;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchWorkerTest {

	@Mock private StringRedisTemplate redisTemplate;
	@Mock private StreamOperations<String, Object, Object> streamOps;
	@Mock private FcmNotificationService fcmService;
	@Mock private NotificationTargetRepository targetRepository;
	@Mock private Counter sentCounter;
	@Mock private Counter failedCounter;

	private NotificationDispatchWorker worker;

	@BeforeEach
	void setUp() {
		lenient().when(redisTemplate.opsForStream()).thenReturn(streamOps);
		// ensureGroup 에서 createGroup 호출 시 이미 존재할 수 있으므로 무시
		lenient().when(streamOps.createGroup(anyString(), anyString())).thenReturn("OK");
		worker = new NotificationDispatchWorker(redisTemplate, fcmService, targetRepository, sentCounter, failedCounter);
	}

	@Test
	void DLQ_크기_조회_스트림_없으면_0() {
		when(streamOps.size("notification:dlq")).thenReturn(null);

		long size = worker.getDlqSize();

		assertThat(size).isEqualTo(0);
	}

	@Test
	void DLQ_크기_조회_정상() {
		when(streamOps.size("notification:dlq")).thenReturn(5L);

		long size = worker.getDlqSize();

		assertThat(size).isEqualTo(5);
	}

	@Test
	void DLQ_재처리_비어있으면_0반환() {
		when(streamOps.read(any(org.springframework.data.redis.connection.stream.StreamOffset.class)))
			.thenReturn(Collections.emptyList());

		int result = worker.replayDlq(10);

		assertThat(result).isEqualTo(0);
	}

	@Test
	void processOnce_빈_스트림이면_즉시_리턴() {
		when(streamOps.read(
			any(org.springframework.data.redis.connection.stream.Consumer.class),
			any(org.springframework.data.redis.connection.stream.StreamReadOptions.class),
			any(org.springframework.data.redis.connection.stream.StreamOffset.class)
		)).thenReturn(null);

		worker.processOnce();

		verify(sentCounter, never()).increment();
		verify(failedCounter, never()).increment();
	}
}
