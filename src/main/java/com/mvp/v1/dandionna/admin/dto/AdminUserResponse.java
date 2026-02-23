package com.mvp.v1.dandionna.admin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.mvp.v1.dandionna.auth.entity.User;
import com.mvp.v1.dandionna.auth.entity.UserRole;

public record AdminUserResponse(
	UUID id,
	String loginId,
	UserRole role,
	LocalDateTime createdAt
) {
	public static AdminUserResponse from(User user) {
		return new AdminUserResponse(
			user.getId(),
			user.getLoginId(),
			user.getRole(),
			user.getCreatedAt()
		);
	}
}
