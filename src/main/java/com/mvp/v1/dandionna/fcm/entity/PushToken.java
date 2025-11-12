package com.mvp.v1.dandionna.fcm.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.mvp.v1.dandionna.auth.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;

@Entity
@Table(name = "push_tokens")
@Getter
public class PushToken {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "platform", nullable = false, columnDefinition = "platform")
	private Platform platform;

	@Column(name = "device_id", nullable = false)
	private String deviceId;

	@Column(name = "fcm_token", nullable = false)
	private String fcmToken;

	@Column(name = "user_agent")
	private String userAgent;

	@Column(name = "last_seen_at", nullable = false)
	private OffsetDateTime lastSeenAt;

	protected PushToken() {
	}

	private PushToken(User user, Platform platform, String deviceId, String fcmToken, String userAgent) {
		this.user = user;
		this.platform = platform;
		this.deviceId = deviceId;
		this.fcmToken = fcmToken;
		this.userAgent = userAgent;
		this.lastSeenAt = OffsetDateTime.now();
	}

	public static PushToken create(User user, Platform platform, String deviceId, String fcmToken, String userAgent) {
		return new PushToken(user, platform, deviceId, fcmToken, userAgent);
	}

	public void updateToken(String fcmToken, String userAgent) {
		this.fcmToken = fcmToken;
		this.userAgent = userAgent;
		touch();
	}

	public void touch() {
		this.lastSeenAt = OffsetDateTime.now();
	}

	public boolean hasSamePayload(String fcmToken, String userAgent) {
		boolean tokenSame = this.fcmToken.equals(fcmToken);
		boolean uaSame = (this.userAgent == null && userAgent == null)
			|| (this.userAgent != null && this.userAgent.equals(userAgent));
		return tokenSame && uaSame;
	}
}
