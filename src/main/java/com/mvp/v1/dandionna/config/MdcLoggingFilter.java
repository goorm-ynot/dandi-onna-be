package com.mvp.v1.dandionna.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 요청 추적을 위한 MDC 필터.
 * traceId, userId, requestUri를 MDC에 설정하여 구조화 로그에 자동 포함한다.
 */
@Component
public class MdcLoggingFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {
		try {
			String traceId = request.getHeader("X-Trace-Id");
			if (traceId == null || traceId.isBlank()) {
				traceId = UUID.randomUUID().toString().substring(0, 8);
			}
			MDC.put("traceId", traceId);
			MDC.put("requestUri", request.getMethod() + " " + request.getRequestURI());

			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
				MDC.put("userId", auth.getName());
			}

			response.setHeader("X-Trace-Id", traceId);
			filterChain.doFilter(request, response);
		} finally {
			MDC.clear();
		}
	}
}
