package com.mvp.v1.dandionna.common.service;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;

/**
 * SecurityContext에서 현재 사용자 ID를 가져오는 공용 유틸리티.
 */
public final class SecurityUtils {

	private SecurityUtils() {}

	public static UUID getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getPrincipal() == null) {
			throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "인증 정보가 없습니다.");
		}
		try {
			return UUID.fromString(authentication.getPrincipal().toString());
		} catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "잘못된 사용자 식별자입니다.");
		}
	}
}
