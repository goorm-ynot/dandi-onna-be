package com.mvp.v1.dandionna.noshow_post.entity;

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
@Table(name = "no_show_post_schedules")
public class NoShowPostSchedule extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "store_id", nullable = false)
	private UUID storeId;

	@Column(name = "requested_by", nullable = false)
	private UUID requestedBy;

	@Column(name = "preset_id")
	private UUID presetId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", columnDefinition = "no_show_schedule_status", nullable = false)
	private NoShowScheduleStatus status = NoShowScheduleStatus.QUEUED;

	@Column(name = "discount_percent", nullable = false)
	private int discountPercent;

	@Column(name = "visit_available_minutes", nullable = false)
	private int visitAvailableMinutes;

	@Column(name = "sale_delay_minutes", nullable = false)
	private int saleDelayMinutes;

	@Column(name = "start_at", nullable = false)
	private OffsetDateTime startAt;

	@Column(name = "expire_at", nullable = false)
	private OffsetDateTime expireAt;

	@Column(name = "published_post_count")
	private Integer publishedPostCount;

	@Column(name = "published_at")
	private OffsetDateTime publishedAt;

	@Column(name = "cancelled_at")
	private OffsetDateTime cancelledAt;

	@Column(name = "error_message")
	private String errorMessage;

	@Column(name = "active", nullable = false)
	private boolean active = true;

	protected NoShowPostSchedule() {
	}

	private NoShowPostSchedule(UUID storeId, UUID requestedBy, UUID presetId, int discountPercent,
		int visitAvailableMinutes, int saleDelayMinutes, OffsetDateTime startAt, OffsetDateTime expireAt) {
		this.storeId = storeId;
		this.requestedBy = requestedBy;
		this.presetId = presetId;
		this.discountPercent = discountPercent;
		this.visitAvailableMinutes = visitAvailableMinutes;
		this.saleDelayMinutes = saleDelayMinutes;
		this.startAt = startAt;
		this.expireAt = expireAt;
	}

	public static NoShowPostSchedule create(UUID storeId, UUID requestedBy, UUID presetId, int discountPercent,
		int visitAvailableMinutes, int saleDelayMinutes, OffsetDateTime startAt, OffsetDateTime expireAt) {
		return new NoShowPostSchedule(
			storeId,
			requestedBy,
			presetId,
			discountPercent,
			visitAvailableMinutes,
			saleDelayMinutes,
			startAt,
			expireAt
		);
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

	public UUID getPresetId() {
		return presetId;
	}

	public NoShowScheduleStatus getStatus() {
		return status;
	}

	public int getDiscountPercent() {
		return discountPercent;
	}

	public int getVisitAvailableMinutes() {
		return visitAvailableMinutes;
	}

	public int getSaleDelayMinutes() {
		return saleDelayMinutes;
	}

	public OffsetDateTime getStartAt() {
		return startAt;
	}

	public OffsetDateTime getExpireAt() {
		return expireAt;
	}

	public Integer getPublishedPostCount() {
		return publishedPostCount;
	}

	public OffsetDateTime getPublishedAt() {
		return publishedAt;
	}

	public OffsetDateTime getCancelledAt() {
		return cancelledAt;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public boolean isActive() {
		return active;
	}

	public boolean isQueued() {
		return status == NoShowScheduleStatus.QUEUED;
	}

	public void markProcessing() {
		this.status = NoShowScheduleStatus.PROCESSING;
		this.errorMessage = null;
	}

	public void markPublished(int publishedPostCount, OffsetDateTime publishedAt) {
		this.status = NoShowScheduleStatus.PUBLISHED;
		this.publishedPostCount = publishedPostCount;
		this.publishedAt = publishedAt;
		this.errorMessage = null;
		this.active = false;
	}

	public void markCancelled(OffsetDateTime cancelledAt) {
		this.status = NoShowScheduleStatus.CANCELLED;
		this.cancelledAt = cancelledAt;
		this.active = false;
		markDeleted(cancelledAt);
	}

	public void markFailed(String errorMessage) {
		this.status = NoShowScheduleStatus.FAILED;
		this.errorMessage = errorMessage;
		this.active = false;
	}

	public void updateWindow(OffsetDateTime startAt, OffsetDateTime expireAt) {
		this.startAt = startAt;
		this.expireAt = expireAt;
	}
}
