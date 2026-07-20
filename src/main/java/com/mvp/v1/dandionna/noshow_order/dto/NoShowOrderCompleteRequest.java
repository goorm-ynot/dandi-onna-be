package com.mvp.v1.dandionna.noshow_order.dto;

import jakarta.validation.constraints.Size;

public record NoShowOrderCompleteRequest(
	@Size(max = 500, message = "메모는 500자 이내로 작성해주세요.")
	String storeMemo
) {}
