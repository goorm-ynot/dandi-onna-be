package com.mvp.v1.dandionna.auth.dto;

import jakarta.validation.constraints.NotBlank;
/**
 * @author rua
 */
/**
 * 로그아웃 시 클라이언트가 보낸 refresh 토큰.
 */
public record LogoutRequest(
	@NotBlank
	String refreshToken
) {}
