package com.mvp.v1.dandionna.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.auth.dto.LoginRequest;
import com.mvp.v1.dandionna.auth.dto.LoginResponse;
import com.mvp.v1.dandionna.auth.dto.RefreshTokenRequest;
import com.mvp.v1.dandionna.auth.dto.RefreshTokenResponse;
import com.mvp.v1.dandionna.auth.dto.SignUpRequest;
import com.mvp.v1.dandionna.auth.service.AuthService;
import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.config.Security.JwtProps;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * @author rua
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth API", description = "사용자 로그인 관리 API")
@Validated
public class AuthController {

	private static final String REFRESH_COOKIE_NAME = "refresh_token";

	private final AuthService authService;
	private final JwtProps jwtProps;

	@Operation(summary = "소비자,사장님 로그인", description = "소비자,사장님 계정으로 로그인합니다.")
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
		LoginResponse response = authService.login(request);

		ResponseCookie cookie = buildRefreshCookie(response.refreshToken(),
			jwtProps.jwt().refreshDays() * 86400L);

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookie.toString())
			.body(ApiResponse.of(response));
	}

	@Operation(summary = "로그아웃", description = "Access/Refresh 토큰을 원자적으로 블랙리스트에 등록해 무효화합니다.")
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<String>> logout(
		@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
		@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String cookieRefreshToken,
		@RequestBody(required = false) RefreshTokenRequest bodyRequest
	) {
		String accessToken = resolveBearer(authorization);
		String refreshToken = resolveRefreshToken(cookieRefreshToken, bodyRequest);
		authService.logout(accessToken, refreshToken);

		ResponseCookie clearCookie = buildRefreshCookie("", 0);

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, clearCookie.toString())
			.body(ApiResponse.of("로그아웃되었습니다."));
	}

	@Operation(summary = "토큰 재발급", description = "유효한 리프레시 토큰으로 Access 토큰을 재발급합니다.")
	@PostMapping("/token/refresh")
	public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(
		@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String cookieRefreshToken,
		@RequestBody(required = false) RefreshTokenRequest bodyRequest
	) {
		String refreshToken = resolveRefreshToken(cookieRefreshToken, bodyRequest);
		String newAccessToken = authService.refresh(refreshToken);
		return ApiResponse.ok(RefreshTokenResponse.of(newAccessToken));
	}

	@Operation(summary = "회원가입", description = "로그인 ID, 비밀번호, 역할을 받아 신규 사용자를 생성합니다.")
	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<String>> signup(@Valid @RequestBody SignUpRequest request) {
		authService.signUp(request);
		return ApiResponse.created("회원가입이 완료되었습니다.");
	}

	/**
	 * HttpOnly, Secure, SameSite=Strict 쿠키를 생성한다.
	 */
	private ResponseCookie buildRefreshCookie(String value, long maxAgeSeconds) {
		return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
			.httpOnly(true)
			.secure(true)
			.sameSite("Strict")
			.path("/api/v1/auth")
			.maxAge(maxAgeSeconds)
			.build();
	}

	/**
	 * 쿠키 또는 요청 본문에서 Refresh 토큰을 추출한다.
	 * 쿠키 우선, 없으면 본문에서 추출한다 (하위 호환).
	 */
	private String resolveRefreshToken(String cookieToken, RefreshTokenRequest bodyRequest) {
		if (cookieToken != null && !cookieToken.isBlank()) {
			return cookieToken;
		}
		if (bodyRequest != null && bodyRequest.refreshToken() != null && !bodyRequest.refreshToken().isBlank()) {
			return bodyRequest.refreshToken();
		}
		throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "리프레시 토큰이 필요합니다.");
	}

	private String resolveBearer(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "Authorization 헤더가 필요합니다.");
		}
		return authorization.substring(7);
	}
}
