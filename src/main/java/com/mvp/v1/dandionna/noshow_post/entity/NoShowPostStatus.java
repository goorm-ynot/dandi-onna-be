package com.mvp.v1.dandionna.noshow_post.entity;

/**
 * 노쇼 게시글 상태 정의
 * draft    : 사장이 임시 저장한 상태(노출 전)
 * open     : 판매 중(소비자가 주문 가능)
 * sold_out : 재고 소진
 * expired  : 유효 시간 경과로 자동 만료
 * canceled : 사장이 취소
 * closed   : 사장이 조기 종료(재고와 무관)
 */
public enum NoShowPostStatus {
	draft,
	open,
	sold_out,
	expired,
	canceled,
	closed
}
