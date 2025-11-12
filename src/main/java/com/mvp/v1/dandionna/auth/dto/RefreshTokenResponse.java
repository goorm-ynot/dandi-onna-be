package com.mvp.v1.dandionna.auth.dto;

/**
 * @author rua
 */
public record RefreshTokenResponse(
	String accessToken
	) {
	public static RefreshTokenResponse of(String accessToken) {
		return new RefreshTokenResponse(
			accessToken
		);
	}
}
