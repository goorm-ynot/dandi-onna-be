package com.mvp.v1.dandionna.config.Security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rate Limiting 설정을 외부 프로퍼티에서 주입받는 클래스.
 *
 * <pre>
 * app:
 *   rate-limit:
 *     login-per-minute: 5
 *     api-per-minute: 100
 *     export-per-hour: 3
 * </pre>
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
	Integer loginPerMinute,
	Integer apiPerMinute,
	Integer exportPerHour
) {
	public RateLimitProperties {
		if (loginPerMinute == null || loginPerMinute <= 0) loginPerMinute = 5;
		if (apiPerMinute == null || apiPerMinute <= 0) apiPerMinute = 100;
		if (exportPerHour == null || exportPerHour <= 0) exportPerHour = 3;
	}
}
