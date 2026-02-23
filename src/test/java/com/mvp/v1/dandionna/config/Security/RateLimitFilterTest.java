package com.mvp.v1.dandionna.config.Security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

	@Mock
	private StringRedisTemplate redisTemplate;
	@Mock
	private ValueOperations<String, String> valueOps;

	private RateLimitFilter filter;

	@BeforeEach
	void setUp() {
		RateLimitProperties props = new RateLimitProperties(5, 100, 3);
		filter = new RateLimitFilter(redisTemplate, props);
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
	}

	@Test
	void 제한_내_요청은_통과() throws Exception {
		when(valueOps.increment(anyString())).thenReturn(1L);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
		request.setRemoteAddr("127.0.0.1");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void 제한_초과_요청은_429() throws Exception {
		when(valueOps.increment(anyString())).thenReturn(6L); // 5회 제한 초과

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
		request.setRemoteAddr("127.0.0.1");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(429);
		assertThat(response.getHeader("Retry-After")).isEqualTo("60");
	}

	@Test
	void Redis_장애_시_요청_통과() throws Exception {
		when(valueOps.increment(anyString())).thenThrow(new RuntimeException("Redis down"));

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/stores");
		request.setRemoteAddr("127.0.0.1");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void Actuator_경로는_제한_없음() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void X_Forwarded_For_헤더로_클라이언트_IP_판별() throws Exception {
		when(valueOps.increment(anyString())).thenReturn(1L);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
		request.setRemoteAddr("10.0.0.1");
		request.addHeader("X-Forwarded-For", "203.0.113.50, 10.0.0.1");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(200);
	}
}
