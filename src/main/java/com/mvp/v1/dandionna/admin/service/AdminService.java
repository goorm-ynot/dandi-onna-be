package com.mvp.v1.dandionna.admin.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mvp.v1.dandionna.admin.dto.AdminStoreResponse;
import com.mvp.v1.dandionna.admin.dto.AdminUserResponse;
import com.mvp.v1.dandionna.auth.repository.UserRepository;
import com.mvp.v1.dandionna.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

/**
 * 관리자 전용 서비스.
 * 사용자 및 매장 목록 조회 기능을 제공한다.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

	private final UserRepository userRepository;
	private final StoreRepository storeRepository;

	@Transactional(readOnly = true)
	public Page<AdminUserResponse> listUsers(int page, int size) {
		return userRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
			.map(AdminUserResponse::from);
	}

	@Transactional(readOnly = true)
	public Page<AdminStoreResponse> listStores(int page, int size) {
		return storeRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
			.map(AdminStoreResponse::from);
	}
}
