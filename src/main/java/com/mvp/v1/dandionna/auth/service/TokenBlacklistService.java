package com.mvp.v1.dandionna.auth.service;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Access/Refresh 토큰 블랙리스트 관리.
 * 토큰 문자열을 키로 저장하고 TTL 을 토큰 남은 수명만큼 지정해 자동 만료시킨다.
 * 원자적 폐기를 위해 Lua 스크립트로 Access + Refresh를 동시에 블랙리스트한다.
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
	private static final String ACCESS_PREFIX = "blacklist:access:";
	private static final String REFRESH_PREFIX = "blacklist:refresh:";

	/**
	 * Access + Refresh 토큰을 원자적으로 블랙리스트하는 Lua 스크립트.
	 * KEYS[1] = access 키, KEYS[2] = refresh 키
	 * ARGV[1] = access TTL(초), ARGV[2] = refresh TTL(초)
	 */
	private static final DefaultRedisScript<Long> ATOMIC_BLACKLIST_SCRIPT =
		new DefaultRedisScript<>(
			"redis.call('SET', KEYS[1], '1', 'EX', tonumber(ARGV[1])) " +
			"redis.call('SET', KEYS[2], '1', 'EX', tonumber(ARGV[2])) " +
			"return 1",
			Long.class
		);

	private final StringRedisTemplate redisTemplate;

	/**
	 * Access + Refresh 토큰을 원자적으로 블랙리스트에 등록한다.
	 * 하나만 등록되고 다른 하나가 실패하는 경쟁 조건을 방지한다.
	 */
	public void blacklistBoth(String accessToken, Duration accessTtl,
		String refreshToken, Duration refreshTtl) {
		String accessKey = ACCESS_PREFIX + accessToken;
		String refreshKey = REFRESH_PREFIX + refreshToken;
		redisTemplate.execute(
			ATOMIC_BLACKLIST_SCRIPT,
			List.of(accessKey, refreshKey),
			String.valueOf(accessTtl.toSeconds()),
			String.valueOf(refreshTtl.toSeconds())
		);
	}

	public void blacklistAccessToken(String token, Duration ttl) {
		redisTemplate.opsForValue().set(ACCESS_PREFIX + token, "1", ttl);
	}

	public boolean isAccessTokenBlacklisted(String token) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(ACCESS_PREFIX + token));
	}

	public void blacklistRefreshToken(String token, Duration ttl) {
		redisTemplate.opsForValue().set(REFRESH_PREFIX + token, "1", ttl);
	}

	public boolean isRefreshTokenBlacklisted(String token) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(REFRESH_PREFIX + token));
	}
}
