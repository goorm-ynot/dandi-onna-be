package com.mvp.v1.dandionna.export_job.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.mvp.v1.dandionna.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "export_jobs")
public class ExportJob extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "store_id", nullable = false)
	private UUID storeId;

	@Column(name = "requested_by", nullable = false)
	private UUID requestedBy;

	@Column(name = "request_hash", nullable = false, length = 64)
	private String requestHash;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", columnDefinition = "export_job_status", nullable = false)
	private ExportJobStatus status = ExportJobStatus.QUEUED;

	@Column(name = "report_type", nullable = false)
	private String reportType = "OWNER_SALES";

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "include_detail", nullable = false)
	private boolean includeDetail = true;

	@Column(name = "row_count")
	private Integer rowCount;

	@Column(name = "file_key")
	private String fileKey;

	@Column(name = "file_expires_at")
	private OffsetDateTime fileExpiresAt;

	@Column(name = "error_message")
	private String errorMessage;

	@Column(name = "format_version", nullable = false)
	private short formatVersion = 1;

	@Column(name = "active", nullable = false)
	private boolean active = true;

	protected ExportJob() {
	}

	private ExportJob(UUID storeId, UUID requestedBy, String requestHash, LocalDate startDate, LocalDate endDate) {
		this.storeId = storeId;
		this.requestedBy = requestedBy;
		this.requestHash = requestHash;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public static ExportJob create(UUID storeId, UUID requestedBy, String requestHash,
		LocalDate startDate, LocalDate endDate) {
		return new ExportJob(storeId, requestedBy, requestHash, startDate, endDate);
	}

	public UUID getId() {
		return id;
	}

	public UUID getStoreId() {
		return storeId;
	}

	public UUID getRequestedBy() {
		return requestedBy;
	}

	public String getRequestHash() {
		return requestHash;
	}

	public ExportJobStatus getStatus() {
		return status;
	}

	public void setStatus(ExportJobStatus status) {
		this.status = status;
	}

	public String getReportType() {
		return reportType;
	}

	public void setReportType(String reportType) {
		this.reportType = reportType;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public boolean isIncludeDetail() {
		return includeDetail;
	}

	public void setIncludeDetail(boolean includeDetail) {
		this.includeDetail = includeDetail;
	}

	public Integer getRowCount() {
		return rowCount;
	}

	public void setRowCount(Integer rowCount) {
		this.rowCount = rowCount;
	}

	public String getFileKey() {
		return fileKey;
	}

	public void setFileKey(String fileKey) {
		this.fileKey = fileKey;
	}

	public OffsetDateTime getFileExpiresAt() {
		return fileExpiresAt;
	}

	public void setFileExpiresAt(OffsetDateTime fileExpiresAt) {
		this.fileExpiresAt = fileExpiresAt;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public short getFormatVersion() {
		return formatVersion;
	}

	public void setFormatVersion(short formatVersion) {
		this.formatVersion = formatVersion;
	}

	public boolean isActive() {
		return active;
	}

	public void deactivate() {
		this.active = false;
	}
}
