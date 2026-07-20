package com.mvp.v1.dandionna.auth.dto;

import jakarta.validation.constraints.NotBlank;
/**
 * @author rua
 */
/**
 * 로그인 요청 DTO.
 */
public record LoginRequest(
	@NotBlank
	String loginId,

	@NotBlank
	String password
) {}
