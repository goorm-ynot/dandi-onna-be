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
@Table(name = "no_show_post_history")
public class NoShowPostHistory extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "post_id", nullable = false)
	private Long postId;

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

	@Column(name = "replaced_at", nullable = false)
	private OffsetDateTime replacedAt;

	@Column(name = "reason", nullable = false)
	private String reason;

	protected NoShowPostHistory() {
	}

	private NoShowPostHistory(Long postId, NoShowPost post, OffsetDateTime replacedAt, String reason) {
		this.postId = postId;
		this.storeId = post.getStoreId();
		this.menuId = post.getMenuId();
		this.pricePercent = post.getPricePercent();
		this.discountedUnitPrice = post.getDiscountedUnitPrice();
		this.originalUnitPrice = post.getOriginalUnitPrice();
		this.qtyTotal = post.getQtyTotal();
		this.qtyRemaining = post.getQtyRemaining();
		this.startAt = post.getStartAt();
		this.expireAt = post.getExpireAt();
		this.status = post.getStatus();
		this.replacedAt = replacedAt;
		this.reason = reason;
	}

	public static NoShowPostHistory from(NoShowPost post, OffsetDateTime replacedAt, String reason) {
		return new NoShowPostHistory(post.getId(), post, replacedAt, reason);
	}
}
