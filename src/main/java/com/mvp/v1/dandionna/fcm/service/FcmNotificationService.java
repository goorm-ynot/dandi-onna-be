package com.mvp.v1.dandionna.fcm.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Transactional;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.mvp.v1.dandionna.fcm.entity.PushToken;
import com.mvp.v1.dandionna.fcm.repository.PushTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FcmNotificationService {

	private static final Logger log = LoggerFactory.getLogger(FcmNotificationService.class);

	private final FirebaseMessaging firebaseMessaging;
	private final PushTokenRepository pushTokenRepository;

	@Transactional
	public void sendToUser(UUID userId, String title, String body, Map<String, String> data) {
		List<PushToken> tokens = pushTokenRepository.findAllByUserId(userId);
		if (tokens.isEmpty()) {
			log.debug("No push tokens found for user {}", userId);
			return;
		}
		Notification notification = Notification.builder()
			.setTitle(title)
			.setBody(body)
			.build();
		for (PushToken token : tokens) {
			Message.Builder builder = Message.builder()
				.setToken(token.getFcmToken())
				.setNotification(notification);
			if (data != null) {
				data.forEach(builder::putData);
			}
			try {
				firebaseMessaging.send(builder.build());
			} catch (FirebaseMessagingException e) {
				handleSendError(token, e);
			}
		}
	}

	private void handleSendError(PushToken token, FirebaseMessagingException e) {
		MessagingErrorCode code = e.getMessagingErrorCode();
		if (code == MessagingErrorCode.UNREGISTERED
			|| code == MessagingErrorCode.INVALID_ARGUMENT) {
			pushTokenRepository.deleteByFcmToken(token.getFcmToken());
			log.info("Removed invalid FCM token {}", token.getId());
		} else {
			log.warn("Failed to send FCM to token {}: {}", token.getId(), e.getMessage());
		}
	}
}
