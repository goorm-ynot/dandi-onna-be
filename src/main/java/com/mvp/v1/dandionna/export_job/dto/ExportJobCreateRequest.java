package com.mvp.v1.dandionna.export_job.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ExportJobCreateRequest(
	@NotBlank(message = "시작일은 필수입니다.")
	@Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "시작일 형식은 yyyy-MM-dd 입니다.")
	String startDate,

	@NotBlank(message = "종료일은 필수입니다.")
	@Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "종료일 형식은 yyyy-MM-dd 입니다.")
	String endDate,

	Boolean includeDetail
) {
}
