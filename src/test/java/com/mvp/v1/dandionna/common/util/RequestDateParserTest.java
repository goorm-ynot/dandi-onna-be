package com.mvp.v1.dandionna.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;

class RequestDateParserTest {

	@Test
	void parseIsoDate_acceptsDashSeparatedDate() {
		LocalDate result = RequestDateParser.parseIsoDate("2026-04-14", "startDate");

		assertThat(result).isEqualTo(LocalDate.of(2026, 4, 14));
	}

	@Test
	void parseIsoDate_rejectsDotSeparatedDate() {
		assertThatThrownBy(() -> RequestDateParser.parseIsoDate("2026.04.14", "startDate"))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> {
				BusinessException businessException = (BusinessException) ex;
				assertThat(businessException.getCode()).isEqualTo(ErrorCode.BAD_REQUEST);
				assertThat(businessException.getDetail()).contains("yyyy-MM-dd");
			});
	}
}
