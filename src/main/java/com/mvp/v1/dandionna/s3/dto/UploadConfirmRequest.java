package com.mvp.v1.dandionna.s3.dto;

import jakarta.validation.constraints.NotBlank;
/**
 * @author rua
 */
public record UploadConfirmRequest(
	@NotBlank String key,
	@NotBlank String etag
) {}
