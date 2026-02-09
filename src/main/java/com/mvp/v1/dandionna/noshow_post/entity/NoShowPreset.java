package com.mvp.v1.dandionna.noshow_post.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.mvp.v1.dandionna.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "no_show_presets")
public class NoShowPreset extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "store_id", nullable = false)
	private UUID storeId;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Column(name = "discount_percent", nullable = false)
	private int discountPercent;

	@Column(name = "visit_available_minutes", nullable = false)
	private int visitAvailableMinutes;

	@Column(name = "sale_delay_minutes", nullable = false)
	private int saleDelayMinutes;

	@Column(name = "is_default", nullable = false)
	private boolean defaultPreset;

	@Column(name = "active", nullable = false)
	private boolean active = true;

	protected NoShowPreset() {
	}

	private NoShowPreset(UUID storeId, String name, int discountPercent, int visitAvailableMinutes,
		int saleDelayMinutes, boolean defaultPreset) {
		this.storeId = storeId;
		this.name = name;
		this.discountPercent = discountPercent;
		this.visitAvailableMinutes = visitAvailableMinutes;
		this.saleDelayMinutes = saleDelayMinutes;
		this.defaultPreset = defaultPreset;
	}

	public static NoShowPreset createDefault(UUID storeId, String name, int discountPercent, int visitAvailableMinutes,
		int saleDelayMinutes) {
		return new NoShowPreset(storeId, name, discountPercent, visitAvailableMinutes, saleDelayMinutes, true);
	}

	public UUID getId() {
		return id;
	}

	public UUID getStoreId() {
		return storeId;
	}

	public String getName() {
		return name;
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

	public boolean isDefaultPreset() {
		return defaultPreset;
	}

	public boolean isActive() {
		return active;
	}

	public void update(String name, int discountPercent, int visitAvailableMinutes, int saleDelayMinutes) {
		this.name = name;
		this.discountPercent = discountPercent;
		this.visitAvailableMinutes = visitAvailableMinutes;
		this.saleDelayMinutes = saleDelayMinutes;
	}

	public void deactivate() {
		this.active = false;
		markDeleted(OffsetDateTime.now());
	}
}
