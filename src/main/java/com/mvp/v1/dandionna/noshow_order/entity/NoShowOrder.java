package com.mvp.v1.dandionna.noshow_order.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.mvp.v1.dandionna.common.entity.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "no_show_orders")
public class NoShowOrder extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "consumer_id", nullable = false)
	private UUID consumerId;

	@Column(name = "store_id", nullable = false)
	private UUID storeId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", columnDefinition = "order_status", nullable = false)
	private NoShowOrderStatus status = NoShowOrderStatus.PENDING;

	@Column(name = "total_price", nullable = false)
	private int totalPrice;

	@Column(name = "visit_time", nullable = false)
	private OffsetDateTime visitTime;

	@Column(name = "store_memo")
	private String storeMemo;

	@Column(name = "menu_names")
	private String menuNames;

	@Column(name = "payment_method", nullable = false)
	private String paymentMethod = "TEST_CARD";

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "payment_status", columnDefinition = "payment_status", nullable = false)
	private NoShowPaymentStatus paymentStatus = NoShowPaymentStatus.PAID;

	@Column(name = "paid_amount", nullable = false)
	private int paidAmount;

	@Column(name = "payment_tx_id")
	private String paymentTxId;

	@Column(name = "payment_memo")
	private String paymentMemo;

	@Column(name = "paid_at")
	private OffsetDateTime paidAt;

	@Column(name = "failed_at")
	private OffsetDateTime failedAt;

	@Column(name = "refunded_at")
	private OffsetDateTime refundedAt;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<NoShowOrderItem> items = new ArrayList<>();

	protected NoShowOrder() {
	}

	private NoShowOrder(UUID consumerId, UUID storeId, int totalPrice, OffsetDateTime visitTime, String storeMemo) {
		this.consumerId = consumerId;
		this.storeId = storeId;
		this.totalPrice = totalPrice;
		this.visitTime = visitTime;
		this.storeMemo = storeMemo;
		this.paidAmount = totalPrice;
		this.paidAt = OffsetDateTime.now();
	}

	public static NoShowOrder create(UUID consumerId, UUID storeId, int totalPrice,
		OffsetDateTime visitTime, String storeMemo) {
		return new NoShowOrder(consumerId, storeId, totalPrice, visitTime, storeMemo);
	}

	public void addItem(NoShowOrderItem item) {
		items.add(item);
		item.setOrder(this);
	}

	public void removeItem(NoShowOrderItem item) {
		items.remove(item);
		item.setOrder(null);
	}

	public Long getId() {
		return id;
	}

	public UUID getConsumerId() {
		return consumerId;
	}

	public UUID getStoreId() {
		return storeId;
	}

	public NoShowOrderStatus getStatus() {
		return status;
	}

	public void setStatus(NoShowOrderStatus status) {
		this.status = status;
	}

	public int getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(int totalPrice) {
		this.totalPrice = totalPrice;
	}

	public OffsetDateTime getVisitTime() {
		return visitTime;
	}

	public void setVisitTime(OffsetDateTime visitTime) {
		this.visitTime = visitTime;
	}

	public String getStoreMemo() {
		return storeMemo;
	}

	public void setStoreMemo(String storeMemo) {
		this.storeMemo = storeMemo;
	}

	public String getMenuNames() {
		return menuNames;
	}

	public void setMenuNames(String menuNames) {
		this.menuNames = menuNames;
	}

	public List<NoShowOrderItem> getItems() {
		return items;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public NoShowPaymentStatus getPaymentStatus() {
		return paymentStatus;
	}

	public void setPaymentStatus(NoShowPaymentStatus paymentStatus) {
		this.paymentStatus = paymentStatus;
	}

	public int getPaidAmount() {
		return paidAmount;
	}

	public void setPaidAmount(int paidAmount) {
		this.paidAmount = paidAmount;
	}

	public String getPaymentTxId() {
		return paymentTxId;
	}

	public void setPaymentTxId(String paymentTxId) {
		this.paymentTxId = paymentTxId;
	}

	public String getPaymentMemo() {
		return paymentMemo;
	}

	public void setPaymentMemo(String paymentMemo) {
		this.paymentMemo = paymentMemo;
	}

	public OffsetDateTime getPaidAt() {
		return paidAt;
	}

	public void setPaidAt(OffsetDateTime paidAt) {
		this.paidAt = paidAt;
	}

	public OffsetDateTime getFailedAt() {
		return failedAt;
	}

	public void setFailedAt(OffsetDateTime failedAt) {
		this.failedAt = failedAt;
	}

	public OffsetDateTime getRefundedAt() {
		return refundedAt;
	}

	public void setRefundedAt(OffsetDateTime refundedAt) {
		this.refundedAt = refundedAt;
	}
}
