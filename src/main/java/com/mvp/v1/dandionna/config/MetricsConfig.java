package com.mvp.v1.dandionna.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 비즈니스 메트릭 등록.
 * Prometheus에서 dandionna_* 네임스페이스로 조회 가능.
 */
@Configuration
public class MetricsConfig {

	@Bean
	public Counter orderCreatedCounter(MeterRegistry registry) {
		return Counter.builder("dandionna.orders.created")
			.description("생성된 노쇼 주문 수")
			.register(registry);
	}

	@Bean
	public Counter notificationSentCounter(MeterRegistry registry) {
		return Counter.builder("dandionna.notifications.sent")
			.description("전송 성공한 알림 수")
			.register(registry);
	}

	@Bean
	public Counter notificationFailedCounter(MeterRegistry registry) {
		return Counter.builder("dandionna.notifications.failed")
			.description("전송 실패한 알림 수 (DLQ 이동)")
			.register(registry);
	}

	@Bean
	public Counter postExpiredCounter(MeterRegistry registry) {
		return Counter.builder("dandionna.posts.expired")
			.description("자동 만료 처리된 게시글 수")
			.register(registry);
	}
}
