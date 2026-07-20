package com.mvp.v1.dandionna.home.service;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mvp.v1.dandionna.home.dto.HomeStoreRequest;
import com.mvp.v1.dandionna.home.dto.HomeStoreResponse;
import com.mvp.v1.dandionna.s3.service.UploadService;
import com.mvp.v1.dandionna.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeStoreService {

	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

	private final StoreRepository storeRepository;
	private final UploadService uploadService;

	@Transactional(readOnly = true)
	public HomeStoreResponse listHomeStores(HomeStoreRequest request) {
		int page = Math.max(request.page(), 0);
		int size = request.size() > 0 ? request.size() : 10;
		Pageable pageable = PageRequest.of(page, size);

		Page<Object[]> rawPage = storeRepository.findNearbyStores(request.lat(), request.lon(), pageable);
		List<HomeStoreResponse.StoreItem> items = rawPage.getContent().stream()
			.map(this::mapRow)
			.toList();

		HomeStoreResponse.PageInfo pageInfo = new HomeStoreResponse.PageInfo(
			rawPage.getNumber(),
			rawPage.getSize(),
			rawPage.getTotalElements(),
			rawPage.getTotalPages(),
			rawPage.hasNext()
		);

		return new HomeStoreResponse(items, pageInfo);
	}

	private HomeStoreResponse.StoreItem mapRow(Object[] row) {
		UUID storeId = (UUID) row[0];
		String name = row[1] != null ? row[1].toString() : "";
		String open = row[2] != null ? row[2].toString() : "";
		String close = row[3] != null ? row[3].toString() : "";
		String imageKey = row[4] != null ? row[4].toString() : null;
		Double distance = row[5] != null ? ((Number) row[5]).doubleValue() : null;

		String imageUrl = null;
		if (imageKey != null) {
			imageUrl = uploadService.presignDownload(imageKey).url();
		}
		return new HomeStoreResponse.StoreItem(
			storeId,
			name,
			imageUrl,
			open,
			close,
			distance != null ? Math.round(distance * 10d) / 10d : 0d
		);
	}
}
