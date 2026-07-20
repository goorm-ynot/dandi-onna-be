package com.mvp.v1.dandionna.s3.dto;

import jakarta.validation.constraints.NotBlank;
/**
 * @author rua
 */
public record PresignedUrlRequest(
	@NotBlank String fileName,
	@NotBlank String contentType
) {}
