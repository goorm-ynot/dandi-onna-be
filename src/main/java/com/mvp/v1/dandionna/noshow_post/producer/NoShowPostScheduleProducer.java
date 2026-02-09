package com.mvp.v1.dandionna.noshow_post.producer;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class NoShowPostScheduleProducer {

	public static final String ZSET_KEY = "noshow:schedule:queue";

	private final StringRedisTemplate redisTemplate;

	public NoShowPostScheduleProducer(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void enqueue(UUID scheduleId, OffsetDateTime startAt) {
		double score = startAt.toInstant().toEpochMilli();
		redisTemplate.opsForZSet().add(ZSET_KEY, scheduleId.toString(), score);
	}

	public void remove(UUID scheduleId) {
		redisTemplate.opsForZSet().remove(ZSET_KEY, scheduleId.toString());
	}
}

