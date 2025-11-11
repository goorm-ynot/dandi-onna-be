package com.mvp.v1.dandionna.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 DTO.
 */
public record LoginRequest(
	@NotBlank
	String loginId,

	@NotBlank
	String password
) {}
