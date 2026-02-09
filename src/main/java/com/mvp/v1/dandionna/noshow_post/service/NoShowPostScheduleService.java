package com.mvp.v1.dandionna.noshow_post.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.menu.repository.MenuRepository;
import com.mvp.v1.dandionna.noshow_post.dto.NoShowBatchCreateRequest;
import com.mvp.v1.dandionna.noshow_post.dto.NoShowPostScheduleCreateRequest;
import com.mvp.v1.dandionna.noshow_post.dto.NoShowPostScheduleCreateResponse;
import com.mvp.v1.dandionna.noshow_post.dto.NoShowPostScheduleDetailResponse;
import com.mvp.v1.dandionna.noshow_post.dto.NoShowPostScheduleListResponse;
import com.mvp.v1.dandionna.noshow_post.entity.NoShowPostSchedule;
import com.mvp.v1.dandionna.noshow_post.entity.NoShowPostScheduleItem;
import com.mvp.v1.dandionna.noshow_post.entity.NoShowPreset;
import com.mvp.v1.dandionna.noshow_post.entity.NoShowScheduleStatus;
import com.mvp.v1.dandionna.noshow_post.producer.NoShowPostScheduleProducer;
import com.mvp.v1.dandionna.noshow_post.repository.NoShowPostScheduleItemRepository;
import com.mvp.v1.dandionna.noshow_post.repository.NoShowPostScheduleRepository;
import com.mvp.v1.dandionna.store.entity.Store;
import com.mvp.v1.dandionna.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoShowPostScheduleService {

	private static final ZoneOffset DB_ZONE = ZoneOffset.UTC;

	private final StoreRepository storeRepository;
	private final MenuRepository menuRepository;
	private final NoShowPresetService noShowPresetService;
	private final NoShowPostScheduleRepository noShowPostScheduleRepository;
	private final NoShowPostScheduleItemRepository noShowPostScheduleItemRepository;
	private final NoShowPostScheduleProducer noShowPostScheduleProducer;
	private final NoShowPostService noShowPostService;

	@Transactional
	public NoShowPostScheduleCreateResponse createSchedule(UUID ownerId, NoShowPostScheduleCreateRequest request) {
		Store store = loadStore(ownerId);
		NoShowPreset preset = noShowPresetService.resolvePreset(store.getId(), request.presetId());
		validateScheduleItems(store.getId(), request.items());

		OffsetDateTime requestedAt = OffsetDateTime.now(DB_ZONE).withSecond(0).withNano(0);
		OffsetDateTime startAt = requestedAt.plusMinutes(preset.getSaleDelayMinutes()).withSecond(0).withNano(0);
		OffsetDateTime expireAt = startAt.plusMinutes(preset.getVisitAvailableMinutes()).withSecond(0).withNano(0);
		validatePresetWindow(startAt, expireAt);

		NoShowPostSchedule schedule = NoShowPostSchedule.create(
			store.getId(),
			ownerId,
			preset.getId(),
			preset.getDiscountPercent(),
			preset.getVisitAvailableMinutes(),
			preset.getSaleDelayMinutes(),
			startAt,
			expireAt
		);
		NoShowPostSchedule saved = noShowPostScheduleRepository.save(schedule);

		List<NoShowPostScheduleItem> items = request.items().stream()
			.map(item -> NoShowPostScheduleItem.create(saved, item.menuId(), item.quantity()))
			.toList();
		noShowPostScheduleItemRepository.saveAll(items);

		enqueueAfterCommit(saved.getId(), saved.getStartAt());
		return new NoShowPostScheduleCreateResponse(
			saved.getId(),
			saved.getStatus(),
			saved.getDiscountPercent(),
			saved.getVisitAvailableMinutes(),
			saved.getSaleDelayMinutes(),
			saved.getStartAt(),
			saved.getExpireAt(),
			saved.getCreatedAt()
		);
	}

	@Transactional(readOnly = true)
	public NoShowPostScheduleListResponse listSchedules(UUID ownerId, int page, int size, NoShowScheduleStatus status) {
		Store store = loadStore(ownerId);
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

		Page<NoShowPostSchedule> schedules = status == null
			? noShowPostScheduleRepository.findByStoreIdAndDeletedAtIsNull(store.getId(), pageable)
			: noShowPostScheduleRepository.findByStoreIdAndStatusAndDeletedAtIsNull(store.getId(), status, pageable);

		Map<UUID, Integer> itemCountMap = loadItemCountMap(schedules.getContent().stream().map(NoShowPostSchedule::getId).toList());

		List<NoShowPostScheduleListResponse.ScheduleSummary> summaries = schedules.getContent().stream()
			.map(schedule -> new NoShowPostScheduleListResponse.ScheduleSummary(
				schedule.getId(),
				schedule.getStatus(),
				schedule.getDiscountPercent(),
				schedule.getVisitAvailableMinutes(),
				schedule.getSaleDelayMinutes(),
				itemCountMap.getOrDefault(schedule.getId(), 0),
				schedule.getPublishedPostCount(),
				schedule.getStartAt(),
				schedule.getExpireAt(),
				schedule.getCreatedAt(),
				schedule.getErrorMessage()
			))
			.toList();

		NoShowPostScheduleListResponse.PageInfo pageInfo = new NoShowPostScheduleListResponse.PageInfo(
			schedules.getNumber(),
			schedules.getSize(),
			schedules.getTotalElements(),
			schedules.getTotalPages(),
			schedules.hasNext()
		);
		return new NoShowPostScheduleListResponse(summaries, pageInfo);
	}

	@Transactional(readOnly = true)
	public NoShowPostScheduleDetailResponse getScheduleDetail(UUID ownerId, UUID scheduleId) {
		NoShowPostSchedule schedule = loadScheduleForOwner(ownerId, scheduleId, false);
		return toDetail(schedule);
	}

	@Transactional
	public NoShowPostScheduleDetailResponse cancelSchedule(UUID ownerId, UUID scheduleId) {
		NoShowPostSchedule schedule = loadScheduleForOwner(ownerId, scheduleId, true);
		if (!schedule.isQueued()) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "대기중인 예약만 취소할 수 있습니다.");
		}
		schedule.markCancelled(OffsetDateTime.now(DB_ZONE));
		noShowPostScheduleRepository.save(schedule);
		noShowPostScheduleProducer.remove(schedule.getId());
		return toDetail(schedule);
	}

	@Transactional
	public NoShowPostScheduleDetailResponse publishNow(UUID ownerId, UUID scheduleId) {
		NoShowPostSchedule schedule = loadScheduleForOwner(ownerId, scheduleId, true);
		if (!schedule.isQueued()) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "대기중인 예약만 즉시 등록할 수 있습니다.");
		}

		Store store = loadStore(ownerId);
		noShowPostScheduleProducer.remove(schedule.getId());

		OffsetDateTime now = OffsetDateTime.now(DB_ZONE).withSecond(0).withNano(0);
		OffsetDateTime expireAt = now.plusMinutes(schedule.getVisitAvailableMinutes()).withSecond(0).withNano(0);
		schedule.updateWindow(now, expireAt);
		schedule.markProcessing();
		noShowPostScheduleRepository.save(schedule);

		try {
			int publishedCount = publishSchedulePosts(store, schedule);
			schedule.markPublished(publishedCount, OffsetDateTime.now(DB_ZONE));
			noShowPostScheduleRepository.save(schedule);
			return toDetail(schedule);
		} catch (Exception e) {
			schedule.markFailed(e.getMessage() != null ? e.getMessage() : "IMMEDIATE_PUBLISH_FAILED");
			noShowPostScheduleRepository.save(schedule);
			throw new BusinessException(ErrorCode.BAD_REQUEST, "즉시 등록에 실패했습니다: " + e.getMessage());
		}
	}

	@Transactional
	public void processDueSchedule(UUID scheduleId) {
		NoShowPostSchedule schedule = noShowPostScheduleRepository.findByIdForUpdate(scheduleId).orElse(null);
		if (schedule == null || !schedule.isActive() || !schedule.isQueued()) {
			return;
		}

		Store store = storeRepository.findById(schedule.getStoreId()).orElse(null);
		if (store == null) {
			schedule.markFailed("매장을 찾을 수 없습니다.");
			noShowPostScheduleRepository.save(schedule);
			return;
		}

		schedule.markProcessing();
		noShowPostScheduleRepository.save(schedule);

		try {
			int publishedCount = publishSchedulePosts(store, schedule);
			schedule.markPublished(publishedCount, OffsetDateTime.now(DB_ZONE));
		} catch (Exception e) {
			schedule.markFailed(e.getMessage() != null ? e.getMessage() : "SCHEDULE_PUBLISH_FAILED");
		}
		noShowPostScheduleRepository.save(schedule);
	}

	private int publishSchedulePosts(Store store, NoShowPostSchedule schedule) {
		List<NoShowPostScheduleItem> scheduleItems = noShowPostScheduleItemRepository
			.findBySchedule_IdOrderByIdAsc(schedule.getId());
		if (scheduleItems.isEmpty()) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "등록할 메뉴가 없습니다.");
		}
		List<NoShowBatchCreateRequest.Item> batchItems = scheduleItems.stream()
			.map(item -> new NoShowBatchCreateRequest.Item(item.getMenuId(), item.getQuantity()))
			.toList();
		return noShowPostService.publishScheduledBatch(
			store,
			schedule.getDiscountPercent(),
			schedule.getStartAt(),
			schedule.getExpireAt(),
			batchItems
		);
	}

	private Map<UUID, Integer> loadItemCountMap(List<UUID> scheduleIds) {
		if (scheduleIds.isEmpty()) {
			return Map.of();
		}
		Map<UUID, Integer> map = new HashMap<>();
		for (Object[] row : noShowPostScheduleItemRepository.countByScheduleIds(scheduleIds)) {
			map.put((UUID)row[0], ((Long)row[1]).intValue());
		}
		return map;
	}

	private NoShowPostScheduleDetailResponse toDetail(NoShowPostSchedule schedule) {
		List<NoShowPostScheduleDetailResponse.Item> items = noShowPostScheduleItemRepository
			.findBySchedule_IdOrderByIdAsc(schedule.getId())
			.stream()
			.map(item -> new NoShowPostScheduleDetailResponse.Item(item.getMenuId(), item.getQuantity()))
			.toList();

		return new NoShowPostScheduleDetailResponse(
			schedule.getId(),
			schedule.getStatus(),
			schedule.getDiscountPercent(),
			schedule.getVisitAvailableMinutes(),
			schedule.getSaleDelayMinutes(),
			schedule.getPublishedPostCount(),
			schedule.getStartAt(),
			schedule.getExpireAt(),
			schedule.getCreatedAt(),
			schedule.getPublishedAt(),
			schedule.getCancelledAt(),
			schedule.getErrorMessage(),
			items
		);
	}

	private void validateScheduleItems(UUID storeId, List<NoShowPostScheduleCreateRequest.Item> items) {
		if (items == null || items.isEmpty()) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "등록할 메뉴를 입력해 주세요.");
		}
		Set<UUID> menuIds = items.stream().map(NoShowPostScheduleCreateRequest.Item::menuId).collect(Collectors.toSet());
		if (menuIds.size() != items.size()) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "동일한 메뉴가 중복되었습니다.");
		}
		for (NoShowPostScheduleCreateRequest.Item item : items) {
			if (item.quantity() <= 0) {
				throw new BusinessException(ErrorCode.LISTING_QTY_INVALID, "수량은 1 이상이어야 합니다.");
			}
		}
		int existsCount = menuRepository.findByStoreIdAndIdIn(storeId, menuIds).size();
		if (existsCount != menuIds.size()) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "존재하지 않는 메뉴가 포함되어 있습니다.");
		}
	}

	private Store loadStore(UUID ownerId) {
		return storeRepository.findByOwnerUserId(ownerId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "매장을 찾을 수 없습니다."));
	}

	private NoShowPostSchedule loadScheduleForOwner(UUID ownerId, UUID scheduleId, boolean forUpdate) {
		Store store = loadStore(ownerId);
		NoShowPostSchedule schedule = forUpdate
			? noShowPostScheduleRepository.findByIdForUpdate(scheduleId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "예약 등록 정보를 찾을 수 없습니다."))
			: noShowPostScheduleRepository.findById(scheduleId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "예약 등록 정보를 찾을 수 없습니다."));

		if (!schedule.getStoreId().equals(store.getId())) {
			throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE, "해당 예약 등록을 조회할 권한이 없습니다.");
		}
		return schedule;
	}

	private void enqueueAfterCommit(UUID scheduleId, OffsetDateTime startAt) {
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					noShowPostScheduleProducer.enqueue(scheduleId, startAt);
				}
			});
			return;
		}
		noShowPostScheduleProducer.enqueue(scheduleId, startAt);
	}

	private void validatePresetWindow(OffsetDateTime startAt, OffsetDateTime expireAt) {
		if (!expireAt.isAfter(startAt)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "방문 가능시간은 판매 대기시간보다 커야 합니다.");
		}
	}
}
