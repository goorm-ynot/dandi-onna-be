package com.mvp.v1.dandionna.fcm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.mvp.v1.dandionna.auth.entity.User;
import com.mvp.v1.dandionna.auth.entity.UserRole;
import com.mvp.v1.dandionna.auth.repository.UserRepository;
import com.mvp.v1.dandionna.fcm.dto.PushTokenRegisterRequest;
import com.mvp.v1.dandionna.fcm.entity.Platform;
import com.mvp.v1.dandionna.fcm.entity.PushToken;
import com.mvp.v1.dandionna.fcm.repository.PushTokenRepository;

@ExtendWith(MockitoExtension.class)
class PushTokenServiceTest {

	@Mock
	private PushTokenRepository pushTokenRepository;
	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private PushTokenService pushTokenService;

	private User user;
	private UUID userId;

	@BeforeEach
	void setUp() {
		user = User.create("ceo", "pw", UserRole.OWNER);
		userId = UUID.randomUUID();
		ReflectionTestUtils.setField(user, "id", userId);
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
	}

	@Test
	void 신규_토큰이면_저장() {
		PushTokenRegisterRequest req = new PushTokenRegisterRequest(
			Platform.ANDROID, "device-1", "fcm-1", "ua");

		when(pushTokenRepository.findByUserPlatformAndDevice(userId, Platform.ANDROID, "device-1"))
			.thenReturn(Optional.empty());

		pushTokenService.register(userId, req);

		verify(pushTokenRepository).save(any(PushToken.class));
	}

	@Test
	void 기존_토큰이_같으면_lastSeen만_갱신() {
		PushToken existing = PushToken.create(user, Platform.ANDROID, "device-1", "fcm-1", "ua");
		OffsetDateTime before = existing.getLastSeenAt();
		when(pushTokenRepository.findByUserPlatformAndDevice(userId, Platform.ANDROID, "device-1"))
			.thenReturn(Optional.of(existing));

		PushTokenRegisterRequest req = new PushTokenRegisterRequest(
			Platform.ANDROID, "device-1", "fcm-1", "ua");

		pushTokenService.register(userId, req);

		verify(pushTokenRepository, never()).save(any());
		assertThat(existing.getLastSeenAt()).isAfter(before);
	}

	@Test
		void 기존_토큰이_다르면_값_갱신() {
		PushToken existing = PushToken.create(user, Platform.ANDROID, "device-1", "old-token", "ua");
		when(pushTokenRepository.findByUserPlatformAndDevice(userId, Platform.ANDROID, "device-1"))
			.thenReturn(Optional.of(existing));

		PushTokenRegisterRequest req = new PushTokenRegisterRequest(
			Platform.ANDROID, "device-1", "new-token", "ua2");

		pushTokenService.register(userId, req);

		assertThat(existing.getFcmToken()).isEqualTo("new-token");
		assertThat(existing.getUserAgent()).isEqualTo("ua2");
	}
}
