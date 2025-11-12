package com.mvp.v1.dandionna.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.mvp.v1.dandionna.auth.dto.LoginRequest;
import com.mvp.v1.dandionna.auth.dto.LoginResponse;
import com.mvp.v1.dandionna.auth.dto.LogoutRequest;
import com.mvp.v1.dandionna.auth.dto.RefreshTokenRequest;
import com.mvp.v1.dandionna.auth.dto.RefreshTokenResponse;
import com.mvp.v1.dandionna.auth.dto.SignUpRequest;
import com.mvp.v1.dandionna.auth.entity.User;
import com.mvp.v1.dandionna.auth.entity.UserRole;
import com.mvp.v1.dandionna.auth.repository.UserRepository;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.config.Security.JwtProps;
import com.mvp.v1.dandionna.config.Security.JweTokenService;
import com.mvp.v1.dandionna.auth.service.TokenBlacklistService;

import io.jsonwebtoken.Claims;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private JweTokenService tokenService;
	@Mock
	private TokenBlacklistService blacklistService;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		JwtProps jwtProps = new JwtProps(
			new JwtProps.Jwt("issuer", 15, 7),
			new JwtProps.Jwe("A256GCM", "dummy")
		);
		authService = new AuthService(userRepository, passwordEncoder, tokenService, jwtProps, blacklistService);
	}

	@Test
	void 로그인_성공() {
		// given: 가입된 사용자와 토큰 발급 결과를 가짜로 구성
		LoginRequest request = new LoginRequest("user1", "plain");
		User user = createUser("user1", UserRole.CONSUMER, "encoded");

		when(userRepository.findByLoginId("user1")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("plain", "encoded")).thenReturn(true);
		when(tokenService.issueAccessToken(any(), any())).thenReturn("access-token");
		when(tokenService.issueRefreshToken(any())).thenReturn("refresh-token");

		// when: 로그인 서비스 호출
		LoginResponse response = authService.login(request);

		// then: 액세스/리프레시 토큰이 기대값으로 내려오는지 검증
		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.refreshToken()).isEqualTo("refresh-token");
	}

	@Test
	void 로그인_비밀번호_오류() {
		// given: 아이디는 존재하지만 비밀번호가 일치하지 않는 상황
		LoginRequest request = new LoginRequest("user1", "plain");
		User user = createUser("user1", UserRole.CONSUMER, "encoded");
		when(userRepository.findByLoginId("user1")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("plain", "encoded")).thenReturn(false);

		// then: BusinessException 이 발생해야 한다
		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(BusinessException.class);
	}

	@Test
	void 회원가입_중복_예외() {
		// given: 동일한 loginId 가 이미 존재
		SignUpRequest request = new SignUpRequest("user1", "plain", UserRole.CONSUMER);
		when(userRepository.existsByLoginId("user1")).thenReturn(true);

		// then: 중복 예외 발생
		assertThatThrownBy(() -> authService.signUp(request))
			.isInstanceOf(BusinessException.class);
	}

	@Test
	void 로그아웃_블랙리스트_등록() {
		// given: 두 토큰이 모두 유효하고 남은 TTL 을 계산할 수 있음
		String accessToken = "access";
		String refreshToken = "refresh";
		LogoutRequest request = new LogoutRequest(refreshToken);

		Claims accessClaims = claimsWithExpiration(Instant.now().plusSeconds(60));
		Claims refreshClaims = claimsWithExpiration(Instant.now().plusSeconds(120));
		when(tokenService.parseClaims(accessToken)).thenReturn(accessClaims);
		when(tokenService.parseClaims(refreshToken)).thenReturn(refreshClaims);

		// when: 로그아웃 수행
		authService.logout(accessToken, request);

		// then: access/refresh 모두 블랙리스트에 등록 요청
		verify(blacklistService).blacklistAccessToken(eq(accessToken), any(Duration.class));
		verify(blacklistService).blacklistRefreshToken(eq(refreshToken), any(Duration.class));
	}

	@Test
	void 토큰_재발급_성공() {
		// given: 블랙리스트에 없는 refresh 토큰과 사용자
		String refreshToken = "refresh";
		UUID userId = UUID.randomUUID();
		RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

		Claims claims = claimsWithExpiration(Instant.now().plusSeconds(3600));
		when(claims.get("typ")).thenReturn("refresh");
		when(claims.getSubject()).thenReturn(userId.toString());

		User user = createUser("user1", UserRole.OWNER, "encoded");
		ReflectionTestUtils.setField(user, "id", userId);

		when(blacklistService.isRefreshTokenBlacklisted(refreshToken)).thenReturn(false);
		when(tokenService.parseClaims(refreshToken)).thenReturn(claims);
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(tokenService.issueAccessToken(any(), any())).thenReturn("new-access");
		when(tokenService.issueRefreshToken(any())).thenReturn("new-refresh");

		// when
		RefreshTokenResponse response = authService.refresh(request);

		// then: 신규 토큰이 발급되고 기존 refresh 토큰은 블랙리스트 처리
		assertThat(response.accessToken()).isEqualTo("new-access");
		verify(blacklistService).blacklistRefreshToken(eq(refreshToken), any(Duration.class));
	}

	@Test
	void 토큰_재발급_블랙리스트이면_예외() {
		// given: 이미 블랙리스트에 올라간 refresh 토큰
		String refreshToken = "blocked";
		RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
		when(blacklistService.isRefreshTokenBlacklisted(refreshToken)).thenReturn(true);

		// then: 예외 발생 및 신규 토큰 발급 로직은 호출되지 않음
		assertThatThrownBy(() -> authService.refresh(request))
			.isInstanceOf(BusinessException.class);

		verify(tokenService, never()).issueAccessToken(any(), any());
	}

	private User createUser(String loginId, UserRole role, String passwordHash) {
		User user = User.create(loginId, passwordHash, role);
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
		return user;
	}

	private Claims claimsWithExpiration(Instant instant) {
		Claims claims = mock(Claims.class);
		when(claims.getExpiration()).thenReturn(java.util.Date.from(instant));
		return claims;
	}
}
