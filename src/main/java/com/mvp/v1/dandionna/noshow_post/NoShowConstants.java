package com.mvp.v1.dandionna.noshow_post;

import java.time.ZoneId;
import java.time.ZoneOffset;

public final class NoShowConstants {
	public static final int MIN_DISCOUNT_PERCENT = 30;
	public static final int MAX_DISCOUNT_PERCENT = 90;
	public static final int MIN_EXPIRE_MINUTES = 0;
	public static final int MAX_EXPIRE_MINUTES = 300;
	public static final int MIN_QUANTITY = 1;
	public static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");
	public static final ZoneOffset DB_ZONE = ZoneOffset.UTC;

	private NoShowConstants() {}
}
