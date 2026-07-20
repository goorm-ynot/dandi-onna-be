package com.mvp.v1.dandionna.fcm.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mvp.v1.dandionna.auth.entity.User;
import com.mvp.v1.dandionna.auth.repository.UserRepository;
import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.fcm.dto.PushTokenRegisterRequest;
import com.mvp.v1.dandionna.fcm.dto.PushTokenRemoveRequest;
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

		pushTokenRepository.findByFcmToken(request.fcmToken())
			.ifPresentOrElse(existing -> reassignToken(user, existing, request),
				() -> pushTokenRepository.findByUserPlatformAndDevice(userId, request.platform(), request.deviceId())
					.ifPresentOrElse(existing -> updateExisting(existing, request),
						() -> createNew(user, request)));
	}

	private void updateExisting(PushToken pushToken, PushTokenRegisterRequest request) {
		pushToken.updateDeviceInfo(request.platform(), request.deviceId());
		pushToken.updateToken(request.fcmToken(), request.userAgent());
	}

	private void reassignToken(User user, PushToken pushToken, PushTokenRegisterRequest request) {
		pushToken.reassignOwner(user);
		updateExisting(pushToken, request);
	}

	private void createNew(User user, PushTokenRegisterRequest request) {
		PushToken entity = PushToken.create(user, request.platform(), request.deviceId(),
			request.fcmToken(), request.userAgent());
		pushTokenRepository.save(entity);
	}

	@Transactional
	public void unregister(UUID userId, PushTokenRemoveRequest request) {
		pushTokenRepository.deleteByUserIdAndDeviceId(userId, request.deviceId());
		pushTokenRepository.deleteByFcmToken(request.fcmToken());
	}

	@Transactional
	public void deleteToken(String fcmToken) {
		pushTokenRepository.deleteByFcmToken(fcmToken);
	}
}
