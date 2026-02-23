package com.mvp.v1.dandionna.notification.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mvp.v1.dandionna.notification.dto.NotificationHistoryResponse;
import com.mvp.v1.dandionna.notification.repository.NotificationTargetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationHistoryService {

	private final NotificationTargetRepository notificationTargetRepository;

	@Transactional(readOnly = true)
	public Page<NotificationHistoryResponse> getHistory(UUID userId, int page, int size) {
		return notificationTargetRepository.findHistoryByUserId(userId, PageRequest.of(page, size));
	}
}
