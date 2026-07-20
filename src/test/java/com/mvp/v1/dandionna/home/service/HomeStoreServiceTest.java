package com.mvp.v1.dandionna.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.sql.Time;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.mvp.v1.dandionna.home.dto.HomeStoreRequest;
import com.mvp.v1.dandionna.home.dto.HomeStoreResponse;
import com.mvp.v1.dandionna.s3.dto.PresignedUrlResponse;
import com.mvp.v1.dandionna.s3.service.UploadService;
import com.mvp.v1.dandionna.store.repository.StoreRepository;

@ExtendWith(MockitoExtension.class)
class HomeStoreServiceTest {

	@Mock
	private StoreRepository storeRepository;
	@Mock
	private UploadService uploadService;

	@InjectMocks
	private HomeStoreService homeStoreService;

	@Test
	void listHomeStores_returnsDistanceSortedStoresWithSignedImages() {
		UUID storeId = UUID.randomUUID();
		Object[] row = new Object[] {
			storeId,
			"청자 소반",
			Time.valueOf("09:00:00"),
			Time.valueOf("20:30:00"),
			"store-image",
			150.4d
		};
		when(storeRepository.findNearbyStores(37.0, 127.0, PageRequest.of(0, 10)))
			.thenReturn(new PageImpl<>(List.<Object[]>of(row), PageRequest.of(0, 10), 1));
		when(uploadService.presignDownload("store-image"))
			.thenReturn(new PresignedUrlResponse("https://example.com/store", "store-image", 300));

		HomeStoreRequest request = new HomeStoreRequest(37.0, 127.0, 0, 10);
		HomeStoreResponse response = homeStoreService.listHomeStores(request);

		assertThat(response.stores()).hasSize(1);
		HomeStoreResponse.StoreItem item = response.stores().get(0);
		assertThat(item.storeId()).isEqualTo(storeId);
		assertThat(item.name()).isEqualTo("청자 소반");
		assertThat(item.openTime()).isEqualTo("09:00:00");
		assertThat(item.distanceMeters()).isEqualTo(150.4d);
		assertThat(item.imageUrl()).isEqualTo("https://example.com/store");
		assertThat(response.pagination().totalElements()).isEqualTo(1);
	}
}
