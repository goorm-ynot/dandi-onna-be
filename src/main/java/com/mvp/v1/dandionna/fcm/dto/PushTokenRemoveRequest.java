package com.mvp.v1.dandionna.fcm.dto;

import jakarta.validation.constraints.NotBlank;

public record PushTokenRemoveRequest(
	@NotBlank
	String deviceId,
	@NotBlank
	String fcmToken
) {}
