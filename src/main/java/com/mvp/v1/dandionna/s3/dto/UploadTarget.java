package com.mvp.v1.dandionna.s3.dto;

import java.util.UUID;

/**
 * @author rua
 */
public enum UploadTarget {
	STORE_IMAGE("stores/%s/%s%s"),
	MENU_IMAGE("menus/%s/%s%s");

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
		String sanitizedRef = sanitize(refId);  // 살균 로직도 이곳으로 이동
		String extension = extractExtension(fileName);
		return String.format(pattern, sanitizedRef, UUID.randomUUID(), extension);
	}

	private String sanitize(String input) {  // private로 이동
		return input.replaceAll("[^a-zA-Z0-9_-]", "");
	}

	private String extractExtension(String fileName) {  // private로 이동
		int lastDot = fileName.lastIndexOf('.');
		return (lastDot == -1) ? "" : fileName.substring(lastDot);
	}
}