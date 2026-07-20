package com.mvp.v1.dandionna.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;

public final class RequestDateParser {

	public static final String DATE_PATTERN = "yyyy-MM-dd";
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern(DATE_PATTERN);

	private RequestDateParser() {
	}

	public static LocalDate parseIsoDate(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + " 값을 입력하세요.");
		}
		try {
			return LocalDate.parse(value, DATE_FORMAT);
		} catch (DateTimeParseException ex) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + " 형식은 " + DATE_PATTERN + " 입니다.");
		}
	}
}
