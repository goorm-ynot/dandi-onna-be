package com.mvp.v1.dandionna.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mvp.v1.dandionna.auth.dto.LoginRequest;
import com.mvp.v1.dandionna.auth.dto.LoginResponse;
import com.mvp.v1.dandionna.auth.dto.LogoutRequest;
import com.mvp.v1.dandionna.auth.dto.RefreshTokenRequest;
import com.mvp.v1.dandionna.auth.dto.SignUpRequest;
import com.mvp.v1.dandionna.auth.entity.User;
import com.mvp.v1.dandionna.auth.repository.UserRepository;
import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.config.Security.JwtProps;
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
	private final JwtProps jwtProps;
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

	@Transactional
	public void logout(String accessToken, LogoutRequest request) {
		Duration accessTtl = remainingTtl(accessToken);
		if (!accessTtl.isZero() && !accessTtl.isNegative()) {
			blacklistService.blacklistAccessToken(accessToken, accessTtl);
		}

		String refreshToken = request.refreshToken();
		Duration refreshTtl = remainingTtl(refreshToken);
		if (!refreshTtl.isZero() && !refreshTtl.isNegative()) {
			blacklistService.blacklistRefreshToken(refreshToken, refreshTtl);
		}
	}

	@Transactional(readOnly = true)
	public LoginResponse refresh(RefreshTokenRequest request) {
		String refreshToken = request.refreshToken();
		if (blacklistService.isRefreshTokenBlacklisted(refreshToken)) {
			throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "블랙리스트 처리된 리프레시 토큰입니다.");
		}

		Claims claims = parseRefreshClaims(refreshToken);
		String userId = claims.getSubject();
		User user = userRepository.findById(UUID.fromString(userId))
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

		Duration remaining = remainingTtl(refreshToken);
		if (!remaining.isZero() && !remaining.isNegative()) {
			blacklistService.blacklistRefreshToken(refreshToken, remaining);
		}

		String newAccess = tokenService.issueAccessToken(user.getId().toString(), user.getRole().name());
		String newRefresh = tokenService.issueRefreshToken(user.getId().toString());
		return LoginResponse.of(newAccess, newRefresh);
	}

	private Duration remainingTtl(String token) {
		try {
			Claims claims = tokenService.parseClaims(token);
			Instant exp = claims.getExpiration().toInstant();
			return Duration.between(Instant.now(), exp);
		} catch (JwtException ex) {
			throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "토큰이 유효하지 않습니다.");
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
