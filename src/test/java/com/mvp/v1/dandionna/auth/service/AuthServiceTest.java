package com.mvp.v1.dandionna.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.mvp.v1.dandionna.auth.dto.LoginRequest;
import com.mvp.v1.dandionna.auth.dto.LoginResponse;
import com.mvp.v1.dandionna.auth.dto.SignUpRequest;
import com.mvp.v1.dandionna.auth.entity.User;
import com.mvp.v1.dandionna.auth.entity.UserRole;
import com.mvp.v1.dandionna.auth.repository.UserRepository;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.config.Security.JweTokenService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

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
		authService = new AuthService(userRepository, passwordEncoder, tokenService, blacklistService);
	}

	@Test
	void 로그인_성공() {
		LoginRequest request = new LoginRequest("user1", "plain");
		User user = createUser("user1", UserRole.CONSUMER, "encoded");

		when(userRepository.findByLoginId("user1")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("plain", "encoded")).thenReturn(true);
		when(tokenService.issueAccessToken(any(), any())).thenReturn("access-token");
		when(tokenService.issueRefreshToken(any())).thenReturn("refresh-token");

		LoginResponse response = authService.login(request);

		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.refreshToken()).isEqualTo("refresh-token");
	}

	@Test
	void 로그인_비밀번호_오류() {
		LoginRequest request = new LoginRequest("user1", "plain");
		User user = createUser("user1", UserRole.CONSUMER, "encoded");
		when(userRepository.findByLoginId("user1")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("plain", "encoded")).thenReturn(false);

		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(BusinessException.class);
	}

	@Test
	void 로그인_존재하지_않는_사용자() {
		LoginRequest request = new LoginRequest("unknown", "plain");
		when(userRepository.findByLoginId("unknown")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(BusinessException.class);
	}

	@Test
	void 회원가입_중복_예외() {
		SignUpRequest request = new SignUpRequest("user1", "plain", UserRole.CONSUMER);
		when(userRepository.existsByLoginId("user1")).thenReturn(true);

		assertThatThrownBy(() -> authService.signUp(request))
			.isInstanceOf(BusinessException.class);
	}

	@Test
	void 로그아웃_원자적_블랙리스트_등록() {
		String accessToken = "access";
		String refreshToken = "refresh";

		Claims accessClaims = claimsWithExpiration(Instant.now().plusSeconds(60));
		Claims refreshClaims = claimsWithExpiration(Instant.now().plusSeconds(120));
		when(tokenService.parseClaims(accessToken)).thenReturn(accessClaims);
		when(tokenService.parseClaims(refreshToken)).thenReturn(refreshClaims);

		authService.logout(accessToken, refreshToken);

		verify(blacklistService).blacklistBoth(
			eq(accessToken), any(Duration.class),
			eq(refreshToken), any(Duration.class)
		);
	}

	@Test
	void 로그아웃_만료된_액세스_토큰일_때_리프레시만_블랙리스트() {
		String accessToken = "expired-access";
		String refreshToken = "refresh";

		when(tokenService.parseClaims(accessToken)).thenThrow(new JwtException("expired"));
		Claims refreshClaims = claimsWithExpiration(Instant.now().plusSeconds(120));
		when(tokenService.parseClaims(refreshToken)).thenReturn(refreshClaims);

		authService.logout(accessToken, refreshToken);

		verify(blacklistService, never()).blacklistBoth(any(), any(), any(), any());
		verify(blacklistService).blacklistRefreshToken(eq(refreshToken), any(Duration.class));
	}

	@Test
	void 토큰_재발급_성공() {
		String refreshToken = "refresh";
		UUID userId = UUID.randomUUID();

		Claims claims = claimsWithExpiration(Instant.now().plusSeconds(3600));
		when(claims.get("typ")).thenReturn("refresh");
		when(claims.getSubject()).thenReturn(userId.toString());

		User user = createUser("user1", UserRole.OWNER, "encoded");
		ReflectionTestUtils.setField(user, "id", userId);

		when(blacklistService.isRefreshTokenBlacklisted(refreshToken)).thenReturn(false);
		when(tokenService.parseClaims(refreshToken)).thenReturn(claims);
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(tokenService.issueAccessToken(any(), any())).thenReturn("new-access");

		String result = authService.refresh(refreshToken);

		assertThat(result).isEqualTo("new-access");
	}

	@Test
	void 토큰_재발급_블랙리스트이면_예외() {
		String refreshToken = "blocked";
		when(blacklistService.isRefreshTokenBlacklisted(refreshToken)).thenReturn(true);

		assertThatThrownBy(() -> authService.refresh(refreshToken))
			.isInstanceOf(BusinessException.class);

		verify(tokenService, never()).issueAccessToken(any(), any());
	}

	@Test
	void 토큰_재발급_리프레시_아닌_토큰_예외() {
		String accessToken = "access-token-not-refresh";

		Claims claims = claimsWithExpiration(Instant.now().plusSeconds(3600));
		when(claims.get("typ")).thenReturn("access");

		when(blacklistService.isRefreshTokenBlacklisted(accessToken)).thenReturn(false);
		when(tokenService.parseClaims(accessToken)).thenReturn(claims);

		assertThatThrownBy(() -> authService.refresh(accessToken))
			.isInstanceOf(BusinessException.class);
	}

	private User createUser(String loginId, UserRole role, String passwordHash) {
		User user = User.create(loginId, passwordHash, role);
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
		return user;
	}

	private Claims claimsWithExpiration(Instant instant) {
		Claims claims = mock(Claims.class);
		lenient().when(claims.getExpiration()).thenReturn(java.util.Date.from(instant));
		return claims;
	}
}
