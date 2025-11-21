package com.mvp.v1.dandionna.favorite.service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import com.mvp.v1.dandionna.favorite.repository.FavoriteRepository;
import com.mvp.v1.dandionna.fcm.service.FcmNotificationService;
import com.mvp.v1.dandionna.menu.entity.Menu;
import com.mvp.v1.dandionna.noshow_post.entity.NoShowPost;
import com.mvp.v1.dandionna.store.entity.Store;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavoriteNotificationService {

	private static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter TIME_FORMATTER =
		DateTimeFormatter.ofPattern("MM월 dd일 HH:mm").withZone(ZONE_KST);

	private final FavoriteRepository favoriteRepository;
	private final FcmNotificationService fcmNotificationService;

	public void notifyNoShowPost(Store store, Menu menu, NoShowPost post) {
		List<UUID> consumerIds = favoriteRepository.findConsumerIdsByStoreId(store.getId());
		if (consumerIds.isEmpty()) {
			return;
		}
		String[] message = pickMessage(store.getName(), menu.getName(), post.getPricePercent());
		Map<String, String> data = new HashMap<>();
		data.put("deeplink", "/customer");
		data.put("storeId", store.getId().toString());
		data.put("menuId", menu.getId().toString());
		data.put("postId", post.getId().toString());
		data.put("visitTime", TIME_FORMATTER.format(post.getExpireAt().atZoneSameInstant(ZONE_KST)));
		data.put("isconsumer", "true");

		for (UUID consumerId : consumerIds) {
			fcmNotificationService.sendToUser(consumerId, message[0], message[1], data);
		}
	}

	private String[] pickMessage(String storeName, String menuName, int discountPercent) {
		boolean first = ThreadLocalRandom.current().nextBoolean();
		String discountText = discountPercent + "%";
		if (first) {
			String title = String.format("%s에서 %s이 주인을 기다려요🥺", storeName, menuName);
			String body = String.format(Locale.KOREA,
				"30분 전 예약 취소로 귀한 잔여 상품이 나왔어요! 즉시 방문하시면 %s 할인가에 만나보세요.", discountText);
			return new String[] { title, body };
		}
		String title = String.format("%s 지금 노쇼 상품 %s할인 찬스", storeName, discountText);
		String body = String.format(Locale.KOREA,
			"방금 취소된 %s 특가! 매장의 품질 그대로, 오늘만 이 가격에 누리세요.", menuName);
		return new String[] { title, body };
	}
}
