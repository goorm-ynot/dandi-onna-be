package com.mvp.v1.dandionna.common.dto;

/**
 * @author rua
 */
public record ApiError(
	ErrorCode code,
	String message,
	String detail,
	String traceId
) {}