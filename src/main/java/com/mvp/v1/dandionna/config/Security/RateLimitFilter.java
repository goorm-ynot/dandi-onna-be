package com.mvp.v1.dandionna.config.Security;

import java.io.IOException;
import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.dto.ErrorCode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Redis 기반 슬라이딩 윈도우 Rate Limiting 필터.
 * 엔드포인트 카테고리별로 다른 제한을 적용한다.
 *
 * <ul>
 *     <li>로그인: IP 기반 5회/분</li>
 *     <li>엑셀 Export: 사용자 기반 3회/시간</li>
 *     <li>일반 API: 사용자(또는 IP) 기반 100회/분</li>
 * </ul>
 */
public class RateLimitFilter extends OncePerRequestFilter {

	private static final String RATE_LIMIT_PREFIX = "ratelimit:";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final StringRedisTemplate redisTemplate;
	private final RateLimitProperties properties;

	public RateLimitFilter(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
		this.redisTemplate = redisTemplate;
		this.properties = properties;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {

		String path = request.getRequestURI();
		String method = request.getMethod();

		// 정적 리소스, 헬스체크, Swagger는 제한 없음
		if (isExcluded(path)) {
			filterChain.doFilter(request, response);
			return;
		}

		String key;
		int maxRequests;
		Duration window;

		if (path.startsWith("/api/v1/auth/login") && "POST".equals(method)) {
			// 로그인: IP 기반 제한
			key = RATE_LIMIT_PREFIX + "login:" + getClientIp(request);
			maxRequests = properties.loginPerMinute();
			window = Duration.ofMinutes(1);
		} else if (path.startsWith("/api/v1/owner/sales/export") && "POST".equals(method)) {
			// 엑셀 Export: 사용자 기반 제한
			String userId = getCurrentUserId();
			if (userId == null) {
				filterChain.doFilter(request, response);
				return;
			}
			key = RATE_LIMIT_PREFIX + "export:" + userId;
			maxRequests = properties.exportPerHour();
			window = Duration.ofHours(1);
		} else {
			// 일반 API: 사용자 또는 IP 기반 제한
			String identifier = getCurrentUserId();
			if (identifier == null) {
				identifier = "ip:" + getClientIp(request);
			}
			key = RATE_LIMIT_PREFIX + "api:" + identifier;
			maxRequests = properties.apiPerMinute();
			window = Duration.ofMinutes(1);
		}

		if (!isAllowed(key, maxRequests, window)) {
			writeRateLimitResponse(response, maxRequests, window);
			return;
		}

		filterChain.doFilter(request, response);
	}

	/**
	 * Redis INCR + EXPIRE 기반 고정 윈도우 카운터.
	 * 원자성을 보장하기 위해 increment 후 TTL이 없으면 설정한다.
	 */
	private boolean isAllowed(String key, int maxRequests, Duration window) {
		try {
			Long count = redisTemplate.opsForValue().increment(key);
			if (count != null && count == 1) {
				redisTemplate.expire(key, window);
			}
			return count != null && count <= maxRequests;
		} catch (Exception e) {
			// Redis 장애 시 요청을 차단하지 않는다 (fail-open)
			return true;
		}
	}

	private void writeRateLimitResponse(HttpServletResponse response, int maxRequests, Duration window)
		throws IOException {
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Retry-After", String.valueOf(window.toSeconds()));
		response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));

		ApiResponse<Void> body = ApiResponse.ofError(ErrorCode.RATE_LIMIT_EXCEEDED, null);
		response.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
	}

	private String getCurrentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
			return auth.getPrincipal().toString();
		}
		return null;
	}

	private String getClientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (xff != null && !xff.isBlank()) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private boolean isExcluded(String path) {
		return path.startsWith("/actuator")
			|| path.startsWith("/swagger-ui")
			|| path.startsWith("/v3/api-docs")
			|| path.startsWith("/api-docs")
			|| path.equals("/favicon.ico")
			|| path.startsWith("/static")
			|| path.startsWith("/public");
	}
}
