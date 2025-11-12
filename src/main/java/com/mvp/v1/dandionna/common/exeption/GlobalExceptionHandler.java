package com.mvp.v1.dandionna.common.exeption;

import java.util.stream.Collectors;

import org.springframework.validation.BindException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.validation.ConstraintViolationException;
import org.springframework.security.access.AccessDeniedException;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.dto.ErrorCode;

/**
 * @author rua
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
		return ApiResponse.error(ex.getCode(), ex.getCode().status(), ex.getDetail());
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<ApiResponse<Void>> handleBind(BindException ex) {
		String detail = ex.getBindingResult().getFieldErrors().stream()
			.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
			.collect(Collectors.joining(", "));
		return ApiResponse.error(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, detail);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
		String detail = ex.getConstraintViolations().stream()
			.map(v -> v.getPropertyPath() + ": " + v.getMessage())
			.collect(Collectors.joining(", "));
		return ApiResponse.error(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, detail);
	}

	@ExceptionHandler({ MissingServletRequestParameterException.class,
		HttpMessageNotReadableException.class,
		MethodArgumentTypeMismatchException.class })
	public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
		return ApiResponse.error(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethod(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
		return ApiResponse.error(ErrorCode.METHOD_NOT_ALLOWED, HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage());
	}

	@ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMedia(org.springframework.web.HttpMediaTypeNotSupportedException ex) {
		return ApiResponse.error(ErrorCode.UNSUPPORTED_MEDIA_TYPE, HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<Void>> handleDenied(AccessDeniedException ex) {
		return ApiResponse.error(ErrorCode.AUTH_FORBIDDEN_ROLE, HttpStatus.FORBIDDEN, ex.getMessage());
	}

	@ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleNoHandler(org.springframework.web.servlet.NoHandlerFoundException ex) {
		return ApiResponse.error(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, ex.getRequestURL());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleAny(Exception ex) {
		return ApiResponse.error(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, ex.getClass().getSimpleName());
	}
}