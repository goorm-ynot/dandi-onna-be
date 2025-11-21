package com.mvp.v1.dandionna.noshow_post.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.service.SecurityUtils;
import com.mvp.v1.dandionna.noshow_post.dto.NoShowBatchCreateRequest;
import com.mvp.v1.dandionna.noshow_post.dto.NoShowPostDetailResponse;
import com.mvp.v1.dandionna.noshow_post.dto.NoShowPostsResponse;
import com.mvp.v1.dandionna.noshow_post.service.NoShowPostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * @author rua
 */

@RestController
@RequestMapping("api/v1/owner/no-show-posts")
@Validated
@RequiredArgsConstructor
public class NoShowPostController {

	private final NoShowPostService noShowPostService;

	@Operation(summary = "노쇼 글 일괄 생성")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping("/batch")
	public ResponseEntity<ApiResponse<String>> createBatch(
		@Valid @RequestBody NoShowBatchCreateRequest request
	) {
		UUID userId = SecurityUtils.getCurrentUserId();
		noShowPostService.createBatch(userId, request);
		return ApiResponse.created("노쇼 글이 등록되었습니다.");
	}

	@Operation(summary = "노쇼 글 목록 조회")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping
	public ResponseEntity<ApiResponse<NoShowPostsResponse>> list(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) {
		UUID userId = SecurityUtils.getCurrentUserId();
		NoShowPostsResponse response = noShowPostService.listPosts(userId, page, size, date);
		return ApiResponse.ok(response);
	}

	@Operation(summary = "노쇼 글 상세 조회")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/{postId}")
	public ResponseEntity<ApiResponse<NoShowPostDetailResponse>> get(
		@PathVariable Long postId
	) {
		UUID userId = SecurityUtils.getCurrentUserId();
		NoShowPostDetailResponse response = noShowPostService.getPostDetail(userId, postId);
		return ApiResponse.ok(response);
	}
}
