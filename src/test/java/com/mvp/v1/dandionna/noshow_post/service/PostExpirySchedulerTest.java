package com.mvp.v1.dandionna.noshow_post.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mvp.v1.dandionna.noshow_post.entity.NoShowPost;
import com.mvp.v1.dandionna.noshow_post.entity.NoShowPostStatus;
import com.mvp.v1.dandionna.noshow_post.repository.NoShowPostRepository;

import io.micrometer.core.instrument.Counter;

@ExtendWith(MockitoExtension.class)
class PostExpirySchedulerTest {

	@Mock
	private NoShowPostRepository noShowPostRepository;
	@Mock
	private Counter postExpiredCounter;

	@InjectMocks
	private PostExpiryScheduler scheduler;

	@Test
	void 만료_게시글_있으면_markExpired_호출() {
		NoShowPost post = org.mockito.Mockito.mock(NoShowPost.class);
		when(noShowPostRepository.findExpiredPosts(eq(NoShowPostStatus.open), any()))
			.thenReturn(List.of(post));

		scheduler.expirePosts();

		verify(post).markExpired();
		verify(postExpiredCounter).increment(1);
	}

	@Test
	void 만료_게시글_없으면_아무것도_안함() {
		when(noShowPostRepository.findExpiredPosts(eq(NoShowPostStatus.open), any()))
			.thenReturn(Collections.emptyList());

		scheduler.expirePosts();

		verify(postExpiredCounter, never()).increment(any(double.class));
	}
}
