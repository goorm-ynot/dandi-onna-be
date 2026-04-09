package com.mvp.v1.dandionna.s3.dto;

public record MenuImageTempConfirmResponse(
	String uploadToken,
	String tempKey,
	String contentType,
	String etag,
	boolean confirmed
) {}
