package com.mvp.v1.dandionna.fcm.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
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
	public boolean sendToUser(UUID userId, String title, String body, Map<String, String> data) {
		List<PushToken> tokens = pushTokenRepository.findAllByUserId(userId);
		if (tokens.isEmpty()) {
			log.debug("No push tokens found for user {}", userId);
			return false;
		}
		return sendMulticast(tokens, title, body, data);
	}

	private boolean sendMulticast(List<PushToken> tokens, String title, String body, Map<String, String> data) {
		List<String> tokenStrings = tokens.stream().map(PushToken::getFcmToken).toList();
		Map<String, String> payload = new java.util.HashMap<>();
		if (data != null) {
			payload.putAll(data);
		}
		payload.put("title", title);
		payload.put("body", body);
		MulticastMessage.Builder builder = MulticastMessage.builder()
			.addAllTokens(tokenStrings);
		payload.forEach(builder::putData);
		try {
			BatchResponse response = firebaseMessaging.sendEachForMulticast(builder.build());
			List<SendResponse> responses = response.getResponses();
			for (int i = 0; i < responses.size(); i++) {
				SendResponse r = responses.get(i);
				if (!r.isSuccessful() && r.getException() != null) {
					handleSendError(tokens.get(i), r.getException());
				}
			}
			return response.getSuccessCount() > 0;
		} catch (FirebaseMessagingException e) {
			log.warn("Failed multicast send: {}", e.getMessage());
			return false;
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
