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

import lombok.Getter;

@Entity
@Table(name = "no_show_posts")
@Getter
public class NoShowPost extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "store_id", nullable = false)
	private UUID storeId;

	@Column(name = "menu_id", nullable = false)
	private UUID menuId;

	@Column(name = "price_percent", nullable = false)
	private int pricePercent;

	@Column(name = "discounted_unit_price", nullable = false)
	private int discountedUnitPrice;

	@Column(name = "original_unit_price")
	private Integer originalUnitPrice;

	@Column(name = "qty_total", nullable = false)
	private int qtyTotal;

	@Column(name = "qty_remaining", nullable = false)
	private int qtyRemaining;

	@Column(name = "start_at", nullable = false)
	private OffsetDateTime startAt;

	@Column(name = "expire_at", nullable = false)
	private OffsetDateTime expireAt;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", columnDefinition = "no_show_post_status", nullable = false)
	private NoShowPostStatus status;

	protected NoShowPost() {
	}

	private NoShowPost(
		UUID storeId,
		UUID menuId,
		int pricePercent,
		int discountedUnitPrice,
		Integer originalUnitPrice,
		int qtyTotal,
		int qtyRemaining,
		OffsetDateTime startAt,
		OffsetDateTime expireAt,
		NoShowPostStatus status
	) {
		this.storeId = storeId;
		this.menuId = menuId;
		this.pricePercent = pricePercent;
		this.discountedUnitPrice = discountedUnitPrice;
		this.originalUnitPrice = originalUnitPrice;
		this.qtyTotal = qtyTotal;
		this.qtyRemaining = qtyRemaining;
		this.startAt = startAt;
		this.expireAt = expireAt;
		this.status = status;
	}

	public static NoShowPost create(
		UUID storeId,
		UUID menuId,
		int pricePercent,
		int discountedUnitPrice,
		Integer originalUnitPrice,
		int quantity,
		OffsetDateTime startAt,
		OffsetDateTime expireAt
	) {
		return new NoShowPost(
			storeId,
			menuId,
			pricePercent,
			discountedUnitPrice,
			originalUnitPrice,
			quantity,
			quantity,
			startAt,
			expireAt,
			NoShowPostStatus.open
		);
	}

	public void overrideListing(
		int pricePercent,
		int discountedUnitPrice,
		Integer originalUnitPrice,
		int quantity,
		OffsetDateTime startAt,
		OffsetDateTime expireAt
	) {
		this.pricePercent = pricePercent;
		this.discountedUnitPrice = discountedUnitPrice;
		this.originalUnitPrice = originalUnitPrice;
		this.qtyTotal = quantity;
		this.qtyRemaining = quantity;
		this.startAt = startAt;
		this.expireAt = expireAt;
		this.status = NoShowPostStatus.open;
	}
}
