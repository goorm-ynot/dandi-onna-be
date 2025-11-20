package com.mvp.v1.dandionna.s3.dto;

/**
 * @author rua
 */
public record PresignedUrlResponse(
	String url,
	String key,
	long expiresInSeconds
) {}
