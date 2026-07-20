package com.mvp.v1.dandionna.auth.dto;

import com.mvp.v1.dandionna.auth.entity.UserRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @author rua
 */
/**
 * 회원가입 요청 DTO.
 */
public record SignUpRequest(
	@NotBlank @Size(min = 4, max = 64)
	String loginId,

	@NotBlank @Size(min = 8, max = 128)
	String password,

	@NotNull
	UserRole role
) {}
