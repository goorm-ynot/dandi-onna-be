package com.mvp.v1.dandionna.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.auth.dto.LoginRequest;
import com.mvp.v1.dandionna.auth.dto.LoginResponse;
import com.mvp.v1.dandionna.auth.dto.LogoutRequest;
import com.mvp.v1.dandionna.auth.dto.RefreshTokenResponse;
import com.mvp.v1.dandionna.auth.dto.SignUpRequest;
import com.mvp.v1.dandionna.auth.dto.RefreshTokenRequest;
import com.mvp.v1.dandionna.auth.service.AuthService;
import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;

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
	private final AuthService authService;

	@Operation(summary = "소비자,사장님 로그인", description = "소비자,사장님 계정으로 로그인합니다.")
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponse>> consumerLogin(@Valid @RequestBody LoginRequest request) {
		var response = authService.login(request);
		return ApiResponse.ok(response);
	}

	@Operation(summary = "로그아웃", description = "Access/Refresh 토큰을 블랙리스트에 등록해 무효화합니다.")
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<String>> logout(
		@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
		@Valid @RequestBody LogoutRequest request) {
		String accessToken = resolveBearer(authorization);
		authService.logout(accessToken, request);
		return ApiResponse.ok("로그아웃되었습니다.");
	}

	@Operation(summary = "토큰 재발급", description = "유효한 리프레시 토큰으로 Access 토큰을 재발급합니다.")
	@PostMapping("/token/refresh")
	public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		var response = authService.refresh(request);
		return ApiResponse.ok(response);
	}

	@Operation(summary = "회원가입", description = "로그인 ID, 비밀번호, 역할을 받아 신규 사용자를 생성합니다.")
	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<String>> signup(@Valid @RequestBody SignUpRequest request) {
		authService.signUp(request);
		return ApiResponse.created("회원가입이 완료되었습니다.");
	}

	private String resolveBearer(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "Authorization 헤더가 필요합니다.");
		}
		return authorization.substring(7);
	}
}
