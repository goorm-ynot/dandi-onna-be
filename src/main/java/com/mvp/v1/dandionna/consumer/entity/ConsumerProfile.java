package com.mvp.v1.dandionna.consumer.entity;

import java.util.UUID;

import com.mvp.v1.dandionna.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "consumer_profiles")
public class ConsumerProfile extends BaseEntity {

	@Id
	@Column(name = "user_id")
	private UUID userId;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "phone", nullable = false)
	private String phone;

	protected ConsumerProfile() {
	}

	public UUID getUserId() {
		return userId;
	}

	public String getName() {
		return name;
	}

	public String getPhone() {
		return phone;
	}
}
