package com.mvp.v1.dandionna.noshow_order.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.mvp.v1.dandionna.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "no_show_order_items")
public class NoShowOrderItem extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	private NoShowOrder order;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "menu_id", nullable = false)
	private UUID menuId;

	@Column(name = "menu_name", nullable = false)
	private String menuName = "";

	@Column(name = "quantity", nullable = false)
	private int quantity;

	@Column(name = "unit_price", nullable = false)
	private int unitPrice;

	@Column(name = "discount_percent", nullable = false)
	private int discountPercent;

	@Column(name = "visit_time", nullable = false)
	private OffsetDateTime visitTime;

	protected NoShowOrderItem() {
	}

	private NoShowOrderItem(Long postId, UUID menuId, String menuName, int quantity, int unitPrice,
		int discountPercent, OffsetDateTime visitTime) {
		this.postId = postId;
		this.menuId = menuId;
		this.menuName = menuName;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
		this.discountPercent = discountPercent;
		this.visitTime = visitTime;
	}

	public static NoShowOrderItem create(Long postId, UUID menuId, String menuName, int quantity, int unitPrice,
		int discountPercent, OffsetDateTime visitTime) {
		return new NoShowOrderItem(postId, menuId, menuName, quantity, unitPrice, discountPercent, visitTime);
	}

	public Long getId() {
		return id;
	}

	public NoShowOrder getOrder() {
		return order;
	}

	void setOrder(NoShowOrder order) {
		this.order = order;
	}

	public Long getPostId() {
		return postId;
	}

	public UUID getMenuId() {
		return menuId;
	}

	public String getMenuName() {
		return menuName;
	}

	public void updateMenuName(String menuName) {
		this.menuName = menuName;
	}

	public int getQuantity() {
		return quantity;
	}

	public int getUnitPrice() {
		return unitPrice;
	}

	public int getDiscountPercent() {
		return discountPercent;
	}

	public OffsetDateTime getVisitTime() {
		return visitTime;
	}

	public void updateQuantity(int quantity) {
		this.quantity = quantity;
	}

	public void updatePricing(int unitPrice, int discountPercent) {
		this.unitPrice = unitPrice;
		this.discountPercent = discountPercent;
	}
}
