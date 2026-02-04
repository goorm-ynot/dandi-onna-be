package com.mvp.v1.dandionna.export_job.dto;

public record ExportJobCreateRequest(
	String startDate,
	String endDate,
	Boolean includeDetail
) {
}
