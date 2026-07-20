package com.mvp.v1.dandionna.noshow_post.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mvp.v1.dandionna.noshow_post.entity.NoShowPost;
import com.mvp.v1.dandionna.noshow_post.entity.NoShowPostStatus;
import com.mvp.v1.dandionna.noshow_post.repository.NoShowPostRepository;

import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;

/**
 * 만료 시간이 지난 노쇼 게시글을 자동으로 EXPIRED 상태로 변경하는 스케줄러.
 * 매 1분마다 실행되어 expire_at < now 이면서 아직 open 상태인 글을 처리한다.
 */
@Component
@RequiredArgsConstructor
public class PostExpiryScheduler {

	private static final Logger log = LoggerFactory.getLogger(PostExpiryScheduler.class);

	private final NoShowPostRepository noShowPostRepository;
	private final Counter postExpiredCounter;

	@Scheduled(fixedRateString = "${app.noshow.post-expiry.poll-interval-ms:60000}")
	@Transactional
	public void expirePosts() {
		List<NoShowPost> expired = noShowPostRepository.findExpiredPosts(
			NoShowPostStatus.open, OffsetDateTime.now());

		if (expired.isEmpty()) return;

		for (NoShowPost post : expired) {
			post.markExpired();
		}
		postExpiredCounter.increment(expired.size());
		log.info("만료 처리된 노쇼 게시글: {}건", expired.size());
	}
}
