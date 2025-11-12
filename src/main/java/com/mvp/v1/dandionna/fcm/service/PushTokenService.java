package com.mvp.v1.dandionna.fcm.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mvp.v1.dandionna.auth.entity.User;
import com.mvp.v1.dandionna.auth.repository.UserRepository;
import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.fcm.dto.PushTokenRegisterRequest;
import com.mvp.v1.dandionna.fcm.entity.PushToken;
import com.mvp.v1.dandionna.fcm.repository.PushTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PushTokenService {

	private final PushTokenRepository pushTokenRepository;
	private final UserRepository userRepository;

	@Transactional
	public void register(UUID userId, PushTokenRegisterRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

		pushTokenRepository.findByUserPlatformAndDevice(userId, request.platform(), request.deviceId())
			.ifPresentOrElse(existing -> updateExisting(existing, request),
				() -> createNew(user, request));
	}

	private void updateExisting(PushToken pushToken, PushTokenRegisterRequest request) {
		if (!pushToken.hasSamePayload(request.fcmToken(), request.userAgent())) {
			pushToken.updateToken(request.fcmToken(), request.userAgent());
		} else {
			pushToken.touch();
		}
	}

	private void createNew(User user, PushTokenRegisterRequest request) {
		PushToken entity = PushToken.create(user, request.platform(), request.deviceId(),
			request.fcmToken(), request.userAgent());
		pushTokenRepository.save(entity);
	}
}
