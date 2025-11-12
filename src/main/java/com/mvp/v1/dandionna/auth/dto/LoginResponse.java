package com.mvp.v1.dandionna.auth.dto;

import java.time.Instant;

/**
 * 로그인 성공 시 내려갈 토큰 정보.
 */
public record LoginResponse(
	String accessToken,
	String refreshToken
) {
	public static LoginResponse of(String accessToken,
		String refreshToken) {
		return new LoginResponse(
			accessToken,
			refreshToken
		);
	}
}
