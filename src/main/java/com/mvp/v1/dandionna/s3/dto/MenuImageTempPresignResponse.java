package com.mvp.v1.dandionna.s3.dto;

public record MenuImageTempPresignResponse(
	String uploadToken,
	String url,
	String tempKey,
	long expiresInSeconds
) {}
