package com.mvp.v1.dandionna.store.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StorePermissionService {

	private final StoreRepository storeRepository;

	public void verifyOwner(UUID ownerId, UUID storeId) {
		boolean exists = storeRepository.findByIdAndOwnerUserId(storeId, ownerId).isPresent();
		if (!exists) {
			throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE, "매장 소유주가 아닙니다.");
		}
	}
}
