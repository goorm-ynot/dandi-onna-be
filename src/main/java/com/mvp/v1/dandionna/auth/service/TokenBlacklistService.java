package com.mvp.v1.dandionna.auth.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Access/Refresh 토큰 블랙리스트 관리.
 * 토큰 문자열을 키로 저장하고 TTL 을 토큰 남은 수명만큼 지정해 자동 만료시킨다.
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
	private static final String ACCESS_PREFIX = "blacklist:access:";
	private static final String REFRESH_PREFIX = "blacklist:refresh:";

	private final StringRedisTemplate redisTemplate;

	public void blacklistAccessToken(String token, Duration ttl) {
		redisTemplate.opsForValue().set(ACCESS_PREFIX + token, "1", ttl);
	}

	public boolean isAccessTokenBlacklisted(String token) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(ACCESS_PREFIX + token));
	}

	public void removeAccessToken(String token) {
		redisTemplate.delete(ACCESS_PREFIX + token);
	}

	public void blacklistRefreshToken(String token, Duration ttl) {
		redisTemplate.opsForValue().set(REFRESH_PREFIX + token, "1", ttl);
	}

	public boolean isRefreshTokenBlacklisted(String token) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(REFRESH_PREFIX + token));
	}

	public void removeRefreshToken(String token) {
		redisTemplate.delete(REFRESH_PREFIX + token);
	}
}
