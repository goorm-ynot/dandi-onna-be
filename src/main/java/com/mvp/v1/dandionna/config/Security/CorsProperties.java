package com.mvp.v1.dandionna.config.Security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * CORS 허용 목록을 외부 설정에서 주입받는 프로퍼티 클래스.
 * {@code application.yaml} 또는 환경변수로 설정한다.
 *
 * <pre>
 * app:
 *   cors:
 *     allowed-origins:
 *       - https://app.dandionna.com
 *       - https://owner.dandionna.com
 *     allowed-methods:
 *       - GET
 *       - POST
 *       - PATCH
 *       - DELETE
 *     allowed-headers:
 *       - Authorization
 *       - Content-Type
 * </pre>
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
	List<String> allowedOrigins,
	List<String> allowedMethods,
	List<String> allowedHeaders
) {
	private static final List<String> DEFAULT_METHODS =
		List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS");
	private static final List<String> DEFAULT_HEADERS =
		List.of("Authorization", "Content-Type", "X-Requested-With");

	public CorsProperties {
		allowedOrigins = sanitize(allowedOrigins);
		allowedMethods = sanitize(allowedMethods);
		allowedHeaders = sanitize(allowedHeaders);
		if (allowedOrigins == null || allowedOrigins.isEmpty()) {
			allowedOrigins = List.of("http://localhost:3000", "http://localhost:5173");
		}
		if (allowedMethods == null || allowedMethods.isEmpty()) {
			allowedMethods = DEFAULT_METHODS;
		}
		if (allowedHeaders == null || allowedHeaders.isEmpty()) {
			allowedHeaders = DEFAULT_HEADERS;
		}
	}

	private static List<String> sanitize(List<String> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream()
			.filter(StringUtils::hasText)
			.map(String::trim)
			.toList();
	}
}
