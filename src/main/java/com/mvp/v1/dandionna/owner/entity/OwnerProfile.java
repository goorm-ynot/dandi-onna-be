package com.mvp.v1.dandionna.owner.entity;

import java.util.UUID;

import com.mvp.v1.dandionna.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "owner_profiles")
public class OwnerProfile extends BaseEntity {

	@Id
	@Column(name = "user_id")
	private UUID userId;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "phone", nullable = false)
	private String phone;

	protected OwnerProfile() {
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
