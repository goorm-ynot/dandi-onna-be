package com.mvp.v1.dandionna.export_job.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.mvp.v1.dandionna.export_job.entity.ExportJobStatus;

public record ExportJobStatusResponse(
	UUID jobId,
	ExportJobStatus status,
	Integer progress,
	String downloadUrl,
	OffsetDateTime expiresAt,
	String errorMessage
) {
}
