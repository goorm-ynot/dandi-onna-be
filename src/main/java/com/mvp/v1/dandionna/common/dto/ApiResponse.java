package com.mvp.v1.dandionna.common.dto;

import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @author rua
 */

public record ApiResponse<T>(
	boolean success,
	String code,      // "OK" 또는 ErrorCode.name()
	String message,   // 사용자용 메시지
	T data            // payload 또는 null
) {
	// ---- payload 전용 팩토리 ----
	public static <T> ApiResponse<T> of(T data) {
		return new ApiResponse<>(true, "OK", "성공", data);
	}
	public static ApiResponse<Void> ofError(ErrorCode code, String message) {
		return new ApiResponse<>(false, code.name(),
			(message != null && !message.isBlank()) ? message : code.defaultMessage(), null);
	}

	// ---- ResponseEntity 헬퍼 (기존 ApiResponses 기능 흡수) ----
	public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
		return ResponseEntity.ok(of(data));
	}
	public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
		return ResponseEntity.status(HttpStatus.CREATED).body(of(data));
	}
	public static ResponseEntity<ApiResponse<Void>> error(ErrorCode code) {
		return error(code, code.status(), null);
	}
	public static ResponseEntity<ApiResponse<Void>> error(ErrorCode code, HttpStatus status, String overrideMsg) {
		String traceId = ensureTraceId();
		return ResponseEntity.status(status)
			.header("X-Trace-Id", traceId)
			.body(ofError(code, overrideMsg));
	}

	private static String ensureTraceId() {
		String id = MDC.get("traceId");
		if (id == null || id.isBlank()) {
			id = UUID.randomUUID().toString();
			MDC.put("traceId", id);
		}
		return id;
	}
}