package com.mvp.v1.dandionna.export_job.producer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 엑셀 내보내기 작업을 Redis Stream에 enqueue하는 프로듀서.
 */
@Component
public class ExportJobProducer {

	private static final String STREAM_KEY = "export:queue";
	private final StringRedisTemplate redisTemplate;

	public ExportJobProducer(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void enqueue(UUID jobId) {
		Map<String, String> map = new HashMap<>();
		map.put("jobId", jobId.toString());
		map.put("attempt", "0");
		redisTemplate.opsForStream().add(STREAM_KEY, map);
	}
}
