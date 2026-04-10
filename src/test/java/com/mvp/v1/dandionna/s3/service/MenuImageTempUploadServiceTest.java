package com.mvp.v1.dandionna.s3.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.s3.dto.MenuImageTempConfirmRequest;
import com.mvp.v1.dandionna.s3.dto.MenuImageTempConfirmResponse;
import com.mvp.v1.dandionna.s3.dto.MenuImageTempPresignResponse;
import com.mvp.v1.dandionna.s3.dto.PresignedUrlRequest;
import com.mvp.v1.dandionna.s3.dto.PresignedUrlResponse;
import com.mvp.v1.dandionna.s3.dto.S3Metadata;
import com.mvp.v1.dandionna.s3.dto.UploadTarget;

@ExtendWith(MockitoExtension.class)
class MenuImageTempUploadServiceTest {

	@Mock
	private StringRedisTemplate redisTemplate;
	@Mock
	private HashOperations<String, Object, Object> hashOperations;
	@Mock
	private ValueOperations<String, String> valueOperations;
	@Mock
	private UploadService uploadService;

	private MenuImageTempUploadService service;

	@BeforeEach
	void setUp() {
		lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
		lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		service = new MenuImageTempUploadService(redisTemplate, uploadService);
		ReflectionTestUtils.setField(service, "tokenTtlSeconds", 1800L);
		ReflectionTestUtils.setField(service, "lockTtlSeconds", 60L);
	}

	@Test
	void presign_storesRedisMetadata() {
		UUID ownerId = UUID.randomUUID();
		PresignedUrlRequest request = new PresignedUrlRequest("americano.png", "image/png");
		when(uploadService.presign(UploadTarget.TEMP_MENU_IMAGE, ownerId.toString(), request))
			.thenReturn(new PresignedUrlResponse("https://upload.example", "temp/menu-images/owner/key.png", 300));

		MenuImageTempPresignResponse response = service.presign(ownerId, request);

		assertThat(response.uploadToken()).isNotBlank();
		assertThat(response.url()).isEqualTo("https://upload.example");
		assertThat(response.expiresInSeconds()).isEqualTo(300);
		verify(redisTemplate).expire(eq("menu:image:temp:" + response.uploadToken()), eq(Duration.ofSeconds(1800)));

		ArgumentCaptor<Map<String, String>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(hashOperations).putAll(eq("menu:image:temp:" + response.uploadToken()), payloadCaptor.capture());
		assertThat(payloadCaptor.getValue())
			.containsEntry("ownerUserId", ownerId.toString())
			.containsEntry("tempKey", "temp/menu-images/owner/key.png")
			.containsEntry("requestedContentType", "image/png")
			.containsEntry("status", "PRESIGNED");
	}

	@Test
	void confirm_marksTokenConfirmed() {
		UUID ownerId = UUID.randomUUID();
		String uploadToken = UUID.randomUUID().toString();
		when(hashOperations.entries("menu:image:temp:" + uploadToken)).thenReturn(Map.of(
			"ownerUserId", ownerId.toString(),
			"tempKey", "temp/menu-images/owner/key.png",
			"requestedContentType", "image/png",
			"status", "PRESIGNED",
			"createdAtEpochSecond", String.valueOf(Instant.now().getEpochSecond())
		));
		when(uploadService.head("temp/menu-images/owner/key.png"))
			.thenReturn(new S3Metadata("temp/menu-images/owner/key.png", "etag-1", "image/png"));

		MenuImageTempConfirmResponse response = service.confirm(ownerId, new MenuImageTempConfirmRequest(uploadToken, "etag-1"));

		assertThat(response.confirmed()).isTrue();
		verify(hashOperations).putAll(eq("menu:image:temp:" + uploadToken), anyMap());
	}

	@Test
	void consume_rejectsUnconfirmedToken() {
		UUID ownerId = UUID.randomUUID();
		String uploadToken = UUID.randomUUID().toString();
		when(valueOperations.setIfAbsent(eq("menu:image:temp:lock:" + uploadToken), eq("1"), eq(Duration.ofSeconds(60))))
			.thenReturn(true);
		when(hashOperations.entries("menu:image:temp:" + uploadToken)).thenReturn(Map.of(
			"ownerUserId", ownerId.toString(),
			"tempKey", "temp/menu-images/owner/key.png",
			"requestedContentType", "image/png",
			"status", "PRESIGNED",
			"createdAtEpochSecond", String.valueOf(Instant.now().getEpochSecond())
		));

		assertThatThrownBy(() -> service.consumeForMenu(ownerId, uploadToken, UUID.randomUUID()))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ErrorCode.MENU_IMAGE_UPLOAD_NOT_CONFIRMED));

		verify(uploadService, never()).copy(anyString(), anyString());
	}

	@Test
	void consume_confirmedTokenCopiesAndCleansUp() {
		UUID ownerId = UUID.randomUUID();
		UUID menuId = UUID.randomUUID();
		String uploadToken = UUID.randomUUID().toString();
		String tempKey = "temp/menu-images/" + ownerId + "/temp.png";

		when(valueOperations.setIfAbsent(eq("menu:image:temp:lock:" + uploadToken), eq("1"), eq(Duration.ofSeconds(60))))
			.thenReturn(true);
		when(hashOperations.entries("menu:image:temp:" + uploadToken)).thenReturn(Map.of(
			"ownerUserId", ownerId.toString(),
			"tempKey", tempKey,
			"requestedContentType", "image/png",
			"confirmedContentType", "image/png",
			"etag", "etag-2",
			"status", "CONFIRMED",
			"createdAtEpochSecond", String.valueOf(Instant.now().getEpochSecond())
		));
		when(redisTemplate.hasKey("menu:image:temp:" + uploadToken)).thenReturn(true);
		when(uploadService.copy(eq(tempKey), anyString()))
			.thenReturn(new S3Metadata("menus/" + menuId + "/final.png", "etag-2", "image/png"));

		S3Metadata result = service.consumeForMenu(ownerId, uploadToken, menuId);

		assertThat(result.getKey()).isEqualTo("menus/" + menuId + "/final.png");
		verify(uploadService).copy(eq(tempKey), anyString());
		verify(uploadService).deleteQuietly(tempKey);
		verify(redisTemplate).delete("menu:image:temp:" + uploadToken);
		verify(redisTemplate).delete("menu:image:temp:lock:" + uploadToken);
	}
}
