package com.mvp.v1.dandionna.fcm.dto;

import com.mvp.v1.dandionna.fcm.entity.Platform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PushTokenRegisterRequest(
	@NotNull
	Platform platform,

	@NotBlank
	String deviceId,

	@NotBlank
	String fcmToken,

	@NotBlank
	String userAgent
) {}
