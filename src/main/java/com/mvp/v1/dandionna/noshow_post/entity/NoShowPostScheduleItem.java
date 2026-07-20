package com.mvp.v1.dandionna.noshow_post.entity;

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
@Table(name = "no_show_post_schedule_items")
public class NoShowPostScheduleItem extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "schedule_id", nullable = false)
	private NoShowPostSchedule schedule;

	@Column(name = "menu_id", nullable = false)
	private UUID menuId;

	@Column(name = "quantity", nullable = false)
	private int quantity;

	protected NoShowPostScheduleItem() {
	}

	private NoShowPostScheduleItem(NoShowPostSchedule schedule, UUID menuId, int quantity) {
		this.schedule = schedule;
		this.menuId = menuId;
		this.quantity = quantity;
	}

	public static NoShowPostScheduleItem create(NoShowPostSchedule schedule, UUID menuId, int quantity) {
		return new NoShowPostScheduleItem(schedule, menuId, quantity);
	}

	public Long getId() {
		return id;
	}

	public NoShowPostSchedule getSchedule() {
		return schedule;
	}

	public UUID getMenuId() {
		return menuId;
	}

	public int getQuantity() {
		return quantity;
	}
}

