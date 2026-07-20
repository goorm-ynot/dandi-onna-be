package com.mvp.v1.dandionna.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mvp.v1.dandionna.auth.dto.LoginRequest;
import com.mvp.v1.dandionna.auth.dto.LoginResponse;
import com.mvp.v1.dandionna.auth.dto.SignUpRequest;
import com.mvp.v1.dandionna.auth.entity.User;
import com.mvp.v1.dandionna.auth.repository.UserRepository;
import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.config.Security.JweTokenService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JweTokenService tokenService;
	private final TokenBlacklistService blacklistService;

	public LoginResponse login(LoginRequest request) {
		User user = userRepository.findByLoginId(request.loginId())
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
		}

		String accessToken = tokenService.issueAccessToken(
			user.getId().toString(), user.getRole().name());
		String refreshToken = tokenService.issueRefreshToken(user.getId().toString());

		return LoginResponse.of(accessToken, refreshToken);
	}

	@Transactional
	public void signUp(SignUpRequest request) {
		if (userRepository.existsByLoginId(request.loginId())) {
			throw new BusinessException(ErrorCode.AUTH_DUPLICATE_LOGIN_ID, "이미 사용 중인 로그인 ID 입니다.");
		}

		String encoded = passwordEncoder.encode(request.password());
		userRepository.save(User.create(request.loginId(), encoded, request.role()));
	}

	/**
	 * Access + Refresh 토큰을 원자적으로 블랙리스트에 등록한다.
	 * Lua 스크립트를 사용하여 두 토큰이 동시에 블랙리스트되거나, 둘 다 실패한다.
	 */
	@Transactional
	public void logout(String accessToken, String refreshToken) {
		Duration accessTtl = safeRemainingTtl(accessToken);
		Duration refreshTtl = safeRemainingTtl(refreshToken);

		if (accessTtl.isPositive() && refreshTtl.isPositive()) {
			blacklistService.blacklistBoth(accessToken, accessTtl, refreshToken, refreshTtl);
		} else if (accessTtl.isPositive()) {
			blacklistService.blacklistAccessToken(accessToken, accessTtl);
		} else if (refreshTtl.isPositive()) {
			blacklistService.blacklistRefreshToken(refreshToken, refreshTtl);
		}
	}

	@Transactional(readOnly = true)
	public String refresh(String refreshToken) {
		if (blacklistService.isRefreshTokenBlacklisted(refreshToken)) {
			throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "블랙리스트 처리된 리프레시 토큰입니다.");
		}

		Claims claims = parseRefreshClaims(refreshToken);
		String userId = claims.getSubject();
		User user = userRepository.findById(UUID.fromString(userId))
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

		return tokenService.issueAccessToken(user.getId().toString(), user.getRole().name());
	}

	private Duration safeRemainingTtl(String token) {
		try {
			Claims claims = tokenService.parseClaims(token);
			Instant exp = claims.getExpiration().toInstant();
			Duration remaining = Duration.between(Instant.now(), exp);
			return remaining.isNegative() ? Duration.ZERO : remaining;
		} catch (JwtException ex) {
			return Duration.ZERO;
		}
	}

	private Claims parseRefreshClaims(String token) {
		try {
			Claims claims = tokenService.parseClaims(token);
			Object typ = claims.get("typ");
			if (!"refresh".equals(typ)) {
				throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "리프레시 토큰이 아닙니다.");
			}
			return claims;
		} catch (JwtException ex) {
			throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "토큰이 유효하지 않습니다.");
		}
	}
}
