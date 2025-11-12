package com.mvp.v1.dandionna.s3.dto;

import java.util.UUID;

/**
 * @author rua
 */
public enum UploadTarget {
	STORE_IMAGE("stores/%s/%s-%s"),
	MENU_IMAGE("menus/%s/%s-%s");

	private final String pattern;

	UploadTarget(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * @param refId 매장/메뉴 ID
	 * @param fileName 원본 파일명 (확장자 포함)
	 * @return S3에 저장될 최종 키
	 */
	public String generateKey(String refId, String fileName) {
		String extension = extractExtension(fileName);
		return String.format(pattern, refId, UUID.randomUUID(), extension);
	}

	private String extractExtension(String fileName) {
		int lastDot = fileName.lastIndexOf('.');
		return (lastDot == -1) ? "" : fileName.substring(lastDot);
	}
}