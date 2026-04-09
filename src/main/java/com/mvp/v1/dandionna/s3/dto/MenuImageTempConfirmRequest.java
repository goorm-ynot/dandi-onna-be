package com.mvp.v1.dandionna.s3.dto;

import jakarta.validation.constraints.NotBlank;

public record MenuImageTempConfirmRequest(
	@NotBlank String uploadToken,
	@NotBlank String etag
) {}
