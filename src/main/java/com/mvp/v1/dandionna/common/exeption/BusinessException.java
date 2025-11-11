package com.mvp.v1.dandionna.common.exeption;

import com.mvp.v1.dandionna.common.dto.ErrorCode;

/**
 * @author rua
 */
public class BusinessException extends RuntimeException {
	private final ErrorCode code;
	private final String detail;
	public BusinessException(ErrorCode code) { super(code.defaultMessage()); this.code = code; this.detail = null; }
	public BusinessException(ErrorCode code, String detail) { super(code.defaultMessage()); this.code = code; this.detail = detail; }
	public ErrorCode getCode() { return code; }
	public String getDetail() { return detail; }
}