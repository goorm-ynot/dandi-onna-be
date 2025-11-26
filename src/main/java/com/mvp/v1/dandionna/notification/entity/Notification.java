package com.mvp.v1.dandionna.notification.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor
public class Notification {

	@Id
	@Column(columnDefinition = "uuid default gen_random_uuid()")
	private UUID id;

	private String title;
	private String body;

	@JdbcTypeCode(SqlTypes.JSON)
	private Object data;

	@Column(name = "created_at")
	private OffsetDateTime createdAt;
}
