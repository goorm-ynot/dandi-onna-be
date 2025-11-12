package com.mvp.v1.dandionna.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 재발급 요청 DTO.
 */
public record RefreshTokenRequest(
	@NotBlank
	String refreshToken
) {}
