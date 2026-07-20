package com.mvp.v1.dandionna.s3.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mvp.v1.dandionna.menu.repository.MenuRepository;
import com.mvp.v1.dandionna.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * S3/MinIO에서 DB에 참조되지 않는 고아 객체를 정리하는 스케줄러.
 * 매일 새벽 3시에 실행되며, 24시간 이상 지난 미참조 객체만 삭제한다.
 */
@Component
@RequiredArgsConstructor
public class OrphanObjectCleanupScheduler {

	private static final Logger log = LoggerFactory.getLogger(OrphanObjectCleanupScheduler.class);

	private final S3Client s3Client;
	private final StoreRepository storeRepository;
	private final MenuRepository menuRepository;

	@Value("${app.s3.bucket}")
	private String bucket;

	@Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시
	public void cleanupOrphanObjects() {
		Set<String> referencedKeys = collectReferencedKeys();
		List<String> orphanKeys = new ArrayList<>();

		Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);

		// stores/ 폴더
		orphanKeys.addAll(findOrphansInPrefix("stores/", referencedKeys, cutoff));
		// menus/ 폴더
		orphanKeys.addAll(findOrphansInPrefix("menus/", referencedKeys, cutoff));
		// temp/menu-images/ 폴더
		orphanKeys.addAll(findObjectsOlderThan("temp/menu-images/", cutoff));

		for (String key : orphanKeys) {
			try {
				s3Client.deleteObject(DeleteObjectRequest.builder()
					.bucket(bucket).key(key).build());
			} catch (Exception e) {
				log.warn("고아 객체 삭제 실패: {}", key, e);
			}
		}

		if (!orphanKeys.isEmpty()) {
			log.info("S3 고아 객체 정리 완료: {}건 삭제", orphanKeys.size());
		}
	}

	private Set<String> collectReferencedKeys() {
		Set<String> keys = storeRepository.findAll().stream()
			.filter(s -> s.getImageKey() != null)
			.map(s -> s.getImageKey())
			.collect(Collectors.toSet());

		menuRepository.findAll().stream()
			.filter(m -> m.getImageKey() != null)
			.map(m -> m.getImageKey())
			.forEach(keys::add);

		return keys;
	}

	private List<String> findOrphansInPrefix(String prefix, Set<String> referencedKeys, Instant cutoff) {
		List<String> orphans = new ArrayList<>();
		String continuationToken = null;

		do {
			ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
				.bucket(bucket)
				.prefix(prefix)
				.maxKeys(1000);

			if (continuationToken != null) {
				requestBuilder.continuationToken(continuationToken);
			}

			ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

			for (S3Object obj : response.contents()) {
				if (!referencedKeys.contains(obj.key())
					&& obj.lastModified().isBefore(cutoff)) {
					orphans.add(obj.key());
				}
			}

			continuationToken = response.isTruncated() ? response.nextContinuationToken() : null;
		} while (continuationToken != null);

		return orphans;
	}

	private List<String> findObjectsOlderThan(String prefix, Instant cutoff) {
		List<String> objects = new ArrayList<>();
		String continuationToken = null;

		do {
			ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
				.bucket(bucket)
				.prefix(prefix)
				.maxKeys(1000);

			if (continuationToken != null) {
				requestBuilder.continuationToken(continuationToken);
			}

			ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

			for (S3Object obj : response.contents()) {
				if (obj.lastModified().isBefore(cutoff)) {
					objects.add(obj.key());
				}
			}

			continuationToken = response.isTruncated() ? response.nextContinuationToken() : null;
		} while (continuationToken != null);

		return objects;
	}
}
