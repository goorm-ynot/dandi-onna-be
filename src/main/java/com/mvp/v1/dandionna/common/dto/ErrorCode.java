package com.mvp.v1.dandionna.common.dto;

import org.springframework.http.HttpStatus;

/**
 * @author rua
 */

	public enum ErrorCode {
		AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "인증 토큰이 유효하지 않습니다."),
		AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "로그인 정보가 올바르지 않습니다."),
		AUTH_FORBIDDEN_ROLE(HttpStatus.FORBIDDEN, "이 작업을 수행할 권한이 없습니다."),
		AUTH_DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 로그인 ID 입니다."),
	STORE_NOT_APPROVED(HttpStatus.CONFLICT, "승인되지 않은 매장입니다."),
	STORE_GEO_REQUIRED(HttpStatus.BAD_REQUEST, "위치 정보가 필요합니다."),
	LISTING_TTL_INVALID(HttpStatus.BAD_REQUEST, "유효시간은 10~300분 사이여야 합니다."),
	LISTING_QTY_INVALID(HttpStatus.BAD_REQUEST, "수량은 1~20 사이여야 합니다."),
	LISTING_PHOTO_REQUIRED(HttpStatus.BAD_REQUEST, "상품 사진 1장이 필요합니다."),
	LISTING_DISCOUNT_INVALID(HttpStatus.BAD_REQUEST, "할인율은 30~90% 사이여야 합니다."),
	LISTING_EXPIRED(HttpStatus.GONE, "이미 만료된 게시물입니다."),
	LISTING_CLOSED(HttpStatus.CONFLICT, "마감된 게시물입니다."),
	ORDER_SINGLE_STORE_ONLY(HttpStatus.BAD_REQUEST, "서로 다른 매장 상품은 함께 주문할 수 없습니다."),
	ORDER_STOCK_SHORTAGE(HttpStatus.CONFLICT, "재고가 부족합니다."),
	PAYMENT_STATE_INVALID(HttpStatus.CONFLICT, "유효하지 않은 결제 상태 전환입니다."),
	PUSH_TOKEN_EXPIRED(HttpStatus.GONE, "만료된 푸시 토큰입니다."),
	PUSH_DELIVERY_FAILED(HttpStatus.BAD_GATEWAY, "푸시 전송에 실패했습니다."),
	BAD_REQUEST(HttpStatus.BAD_REQUEST, "요청이 올바르지 않습니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 HTTP 메서드입니다."),
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 컨텐츠 타입입니다."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");

	private final HttpStatus status;
	private final String defaultMessage;
	ErrorCode(HttpStatus status, String defaultMessage) { this.status = status; this.defaultMessage = defaultMessage; }
	public HttpStatus status() { return status; }
	public String defaultMessage() { return defaultMessage; }
}
