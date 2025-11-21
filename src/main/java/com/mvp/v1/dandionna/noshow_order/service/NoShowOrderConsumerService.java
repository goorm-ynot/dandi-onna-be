package com.mvp.v1.dandionna.noshow_order.service;

import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.consumer.entity.ConsumerProfile;
import com.mvp.v1.dandionna.consumer.repository.ConsumerProfileRepository;
import com.mvp.v1.dandionna.fcm.service.FcmNotificationService;
import com.mvp.v1.dandionna.menu.entity.Menu;
import com.mvp.v1.dandionna.menu.repository.MenuRepository;
import com.mvp.v1.dandionna.noshow_order.dto.NoShowOrderCreateRequest;
import com.mvp.v1.dandionna.noshow_order.dto.NoShowOrderCreateResponse;
import com.mvp.v1.dandionna.noshow_order.entity.NoShowOrder;
import com.mvp.v1.dandionna.noshow_order.entity.NoShowOrderItem;
import com.mvp.v1.dandionna.noshow_order.entity.NoShowPaymentStatus;
import com.mvp.v1.dandionna.noshow_order.repository.NoShowOrderRepository;
import com.mvp.v1.dandionna.noshow_post.entity.NoShowPost;
import com.mvp.v1.dandionna.noshow_post.entity.NoShowPostStatus;
import com.mvp.v1.dandionna.noshow_post.repository.NoShowPostRepository;
import com.mvp.v1.dandionna.store.entity.Store;
import com.mvp.v1.dandionna.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoShowOrderConsumerService {

	private static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter VISIT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
		.withZone(ZONE_KST);

	private final StoreRepository storeRepository;
	private final NoShowPostRepository noShowPostRepository;
	private final MenuRepository menuRepository;
	private final NoShowOrderRepository noShowOrderRepository;
	private final ConsumerProfileRepository consumerProfileRepository;
	private final FcmNotificationService fcmNotificationService;

	@Transactional
	public NoShowOrderCreateResponse createOrder(UUID consumerId, NoShowOrderCreateRequest request) {
		if (request == null || CollectionUtils.isEmpty(request.items())) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "주문 항목이 비어 있습니다.");
		}
		UUID storeId = request.storeId();
		Store store = storeRepository.findById(storeId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "매장을 찾을 수 없습니다."));

		List<Long> postIds = request.items().stream()
			.map(NoShowOrderCreateRequest.Item::noShowPostId)
			.toList();
		Map<Long, NoShowPost> posts = noShowPostRepository.findAllByIdInForUpdate(postIds).stream()
			.collect(Collectors.toMap(NoShowPost::getId, Function.identity()));

		if (posts.size() != postIds.stream().distinct().count()) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "일부 노쇼 글을 찾을 수 없습니다.");
		}

		Map<UUID, Menu> menus = loadMenus(storeId, posts.values());
		OffsetDateTime visitTime = request.visitTime();

		int totalExpected = 0;
		int originalExpected = 0;
		List<Line> lines = new ArrayList<>();

		for (NoShowOrderCreateRequest.Item item : request.items()) {
			NoShowPost post = posts.get(item.noShowPostId());
			validatePost(storeId, visitTime, item, post);

			Menu menu = menus.get(post.getMenuId());
			if (menu == null) {
				throw new BusinessException(ErrorCode.NOT_FOUND, "메뉴 정보를 찾을 수 없습니다.");
			}

			int unitPrice = post.getDiscountedUnitPrice();
			int originalPrice = post.getOriginalUnitPrice() != null
				? post.getOriginalUnitPrice()
				: menu.getPriceKrw();
			if (!Objects.equals(item.originalPrice(), originalPrice)) {
				throw new BusinessException(ErrorCode.BAD_REQUEST, "원가 정보가 일치하지 않습니다.");
			}

			int qty = item.quantity();
			totalExpected += unitPrice * qty;
			originalExpected += originalPrice * qty;

			lines.add(new Line(post, menu, qty, unitPrice));
		}

		if (totalExpected != request.totalAmount()) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "결제 금액이 일치하지 않습니다.");
		}
		if (originalExpected - totalExpected != request.appliedDiscountAmount()) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "할인 금액이 일치하지 않습니다.");
		}

		NoShowOrder order = NoShowOrder.create(consumerId, storeId, originalExpected, visitTime, null);
		order.setPaymentMethod(request.paymentMethod());
		order.setPaymentStatus(NoShowPaymentStatus.PAID);
		order.setTotalPrice(originalExpected);
		order.setPaidAmount(totalExpected);
		order.setPaymentTxId(UUID.randomUUID().toString());
		order.setPaymentMemo("테스트 결제 완료");
		order.setMenuNames(buildMenuSummary(lines));

		for (Line line : lines) {
			NoShowOrderItem item = NoShowOrderItem.create(
				line.post().getId(),
				line.menu().getId(),
				line.menu().getName(),
				line.quantity(),
				line.unitPrice(),
				line.post().getPricePercent(),
				line.post().getExpireAt()
			);
			order.addItem(item);
			line.post().consumeQuantity(line.quantity());
		}

		NoShowOrder saved = noShowOrderRepository.save(order);
		ConsumerProfile consumerProfile = consumerProfileRepository.findById(consumerId).orElse(null);
		notifyOwner(store, saved, consumerProfile);

		return new NoShowOrderCreateResponse(
			saved.getId(),
			saved.getStatus(),
			saved.getPaymentStatus(),
			saved.getPaidAmount(),
			saved.getVisitTime(),
			saved.getMenuNames()
		);
	}

	private void validatePost(UUID storeId, OffsetDateTime visitTime, NoShowOrderCreateRequest.Item item,
		NoShowPost post) {
		if (post == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "노쇼 글을 찾을 수 없습니다.");
		}
		if (!post.getStoreId().equals(storeId)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "다른 매장의 노쇼 글이 포함되어 있습니다.");
		}
		if (post.getStatus() != NoShowPostStatus.open) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "판매 중이 아닌 노쇼 글입니다.");
		}
		if (!post.getExpireAt().toInstant().equals(visitTime.toInstant())) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "방문 시간이 일치하지 않습니다.");
		}
		if (post.getQtyRemaining() < item.quantity()) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "재고가 부족한 메뉴가 있습니다.");
		}
		if (!Objects.equals(post.getPricePercent(), item.discountRate())) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "할인율 정보가 일치하지 않습니다.");
		}
	}

	private Map<UUID, Menu> loadMenus(UUID storeId, java.util.Collection<NoShowPost> posts) {
		List<UUID> menuIds = posts.stream()
			.map(NoShowPost::getMenuId)
			.distinct()
			.toList();
		List<Menu> menus = menuRepository.findByStoreIdAndIdIn(storeId, menuIds);
		if (menus.size() != menuIds.size()) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "일부 메뉴를 찾을 수 없습니다.");
		}
		return menus.stream().collect(Collectors.toMap(Menu::getId, Function.identity()));
	}

	private String buildMenuSummary(List<Line> lines) {
		return lines.stream()
			.map(line -> line.menu().getName() + "(" + line.quantity() + ")")
			.collect(Collectors.joining(", "));
	}

	private void notifyOwner(Store store, NoShowOrder order, ConsumerProfile consumerProfile) {
		if (store.getOwnerUserId() == null) {
			return;
		}
		String consumerName = consumerProfile != null ? consumerProfile.getName() : "고객";
		String consumerPhone = consumerProfile != null ? consumerProfile.getPhone() : "미등록";
		String title = String.format("[노쇼 주문] %s 님이 주문했어요", consumerName);

		String body = String.format("주문번호 #%d, %s, 결제 %s원, 방문 %s, 연락처 %s",
			order.getId(),
			order.getMenuNames(),
			NumberFormat.getInstance(Locale.KOREA).format(order.getPaidAmount()),
			VISIT_TIME_FORMATTER.format(order.getVisitTime().atZoneSameInstant(ZONE_KST)),
			consumerPhone
		);
		Map<String, String> data = new HashMap<>();
		data.put("deeplink", "/seller/order");
		data.put("isconsumer", "false");
		fcmNotificationService.sendToUser(store.getOwnerUserId(), title, body, data);
	}

	private record Line(NoShowPost post, Menu menu, int quantity, int unitPrice) {}
}
