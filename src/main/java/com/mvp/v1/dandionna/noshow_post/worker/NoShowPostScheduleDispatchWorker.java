package com.mvp.v1.dandionna.noshow_post.worker;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.mvp.v1.dandionna.noshow_post.producer.NoShowPostScheduleProducer;
import com.mvp.v1.dandionna.noshow_post.service.NoShowPostScheduleService;

@Component
public class NoShowPostScheduleDispatchWorker {

	private static final Logger log = LoggerFactory.getLogger(NoShowPostScheduleDispatchWorker.class);

	private final StringRedisTemplate redisTemplate;
	private final NoShowPostScheduleService noShowPostScheduleService;

	@Value("${app.noshow.schedule.batch-size:20}")
	private int batchSize;

	public NoShowPostScheduleDispatchWorker(StringRedisTemplate redisTemplate,
		NoShowPostScheduleService noShowPostScheduleService) {
		this.redisTemplate = redisTemplate;
		this.noShowPostScheduleService = noShowPostScheduleService;
	}

	public void processOnce() {
		long nowMillis = Instant.now().toEpochMilli();
		Set<String> dueIds = redisTemplate.opsForZSet().rangeByScore(
			NoShowPostScheduleProducer.ZSET_KEY,
			0,
			nowMillis,
			0,
			Math.max(batchSize, 1)
		);
		if (dueIds == null || dueIds.isEmpty()) {
			return;
		}

		for (String rawId : dueIds) {
			try {
				UUID scheduleId = UUID.fromString(rawId);
				Long removed = redisTemplate.opsForZSet().remove(NoShowPostScheduleProducer.ZSET_KEY, rawId);
				if (removed == null || removed == 0L) {
					continue;
				}
				noShowPostScheduleService.processDueSchedule(scheduleId);
			} catch (Exception e) {
				log.error("Failed to process no-show schedule id={}: {}", rawId, e.getMessage(), e);
			}
		}
	}
}

