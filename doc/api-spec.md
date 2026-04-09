# API 명세 (요약)

> JWT Bearer 인증 필요(표기 없는 일부 공용 제외). 응답은 `ApiResponse<T>` 래퍼 사용.

## 인증
- `POST /api/v1/auth/login` (loginId, password) -> accessToken, refreshToken
- `POST /api/v1/auth/token/refresh` (refreshToken) -> accessToken
- `POST /api/v1/auth/logout` (refreshToken)

## 매장/메뉴 (사장님)
- `POST /api/v1/stores` / `PATCH /api/v1/stores`
- `GET /api/v1/stores/me`
- `GET /api/v1/stores/me/mypage` (storeName/ownerName/주소/영업시간 요약)
- `POST /api/v1/owner/menus`
- `PATCH /api/v1/owner/menus/{menuId}`
- `POST /api/v1/owner/menus/{menuId}/status`
- `DELETE /api/v1/owner/menus/{menuId}`
- `GET /api/v1/owner/menus/{menuId}`
- `GET /api/v1/owner/menus?page&size&keyword&type&effectiveStatus`
- 업로드: `POST /api/v1/stores/uploads/presign|confirm`, `GET /api/v1/stores/uploads/view`
- 메뉴 이미지 임시 업로드: `POST /api/v1/owner/menu-images/temp/presign|confirm`, `GET /api/v1/menus/{id}/uploads/view`

### 메뉴 관리 상세
- 모든 메뉴 API는 `OWNER` 권한과 Bearer 인증이 필요합니다.
- 생성된 메뉴의 저장 상태는 항상 `SOLD_OUT`으로 시작합니다.
- `status` 변경은 일반 수정 API가 아니라 전용 API `POST /api/v1/owner/menus/{menuId}/status` 로만 처리합니다.
- 세트 메뉴는 같은 매장의 `SINGLE` 메뉴만 1개 이상 포함할 수 있습니다. 세트 안에 세트를 넣는 중첩 구성은 허용하지 않습니다.
- 메뉴 이미지를 바꾸는 경우에는 먼저 임시 업로드 API로 `imageUploadToken` 을 발급/확정한 뒤, 생성 또는 수정 요청에 그 토큰을 포함합니다.
- `imageUploadToken` 이 없으면 이미지 처리는 건너뛰고 다른 필드만 생성 또는 수정합니다.

#### 0) 메뉴 이미지 임시 업로드
- `POST /api/v1/owner/menu-images/temp/presign`
- 요청 필드
  - `fileName`, `contentType`
- 응답 필드
  - `uploadToken`, `url`, `tempKey`, `expiresInSeconds`
- 처리 순서
  - presign 발급
  - 반환된 `url` 로 파일 PUT
  - `POST /api/v1/owner/menu-images/temp/confirm`
  - 생성 또는 수정 API에 `imageUploadToken` 포함

```json
{
  "fileName": "americano.png",
  "contentType": "image/png"
}
```

```json
{
  "uploadToken": "UUID",
  "url": "https://object-storage.example/presigned-put-url",
  "tempKey": "temp/menu-images/owner-id/random.png",
  "expiresInSeconds": 300
}
```

#### 0-1) 메뉴 이미지 임시 업로드 Confirm
- `POST /api/v1/owner/menu-images/temp/confirm`
- 요청 바디: `{ "uploadToken": "UUID", "etag": "etag-value" }`
- 응답 필드
  - `uploadToken`, `tempKey`, `contentType`, `etag`, `confirmed`

#### 1) 메뉴 생성
- `POST /api/v1/owner/menus`
- 요청 필드
  - `name`, `priceKrw`
  - `description`: 선택 입력, 미입력 가능
  - `type`: `SINGLE` 또는 `SET`
  - `type=SET` 이면 `components[{ menuId, quantity }]` 필수
  - `type=SINGLE` 이면 `components` 전송 금지
  - 선택 이미지 토큰: `imageUploadToken`
- 응답 필드
  - `id`, `name`, `description`, `priceKrw`
  - `imageKey`, `imageMime`, `imageEtag`, `imageStatus`
  - `type`, `status`, `effectiveStatus`
  - `components[]`

```json
{
  "name": "점심 세트",
  "description": "단품 2종 구성",
  "priceKrw": 12900,
  "type": "SET",
  "imageUploadToken": "UUID",
  "components": [
    { "menuId": "UUID", "quantity": 1 },
    { "menuId": "UUID", "quantity": 2 }
  ]
}
```

#### 2) 메뉴 일반 수정
- `PATCH /api/v1/owner/menus/{menuId}`
- 수정 가능 필드
  - `name`, `description`, `priceKrw`
  - `imageUploadToken`
  - 세트 메뉴일 때만 `components`
- 제약
  - `status` 수정 불가
  - `type` 수정 불가
  - `components` 가 오면 세트 구성을 전체 교체합니다.
  - `description` 은 선택 수정 필드입니다.
  - `description` 이 미전송, `null`, 공백 문자열이면 기존 설명을 유지합니다.
  - `imageUploadToken` 이 없으면 기존 이미지 메타를 유지합니다.
  - 이미지 삭제 전용 기능은 현재 제공하지 않습니다.

#### 3) 메뉴 상태 전용 변경
- `POST /api/v1/owner/menus/{menuId}/status`
- 요청 바디: `{ "onSale": true }` 또는 `{ "onSale": false }`
- 응답 필드
  - `menuId`
  - `status`: 저장 상태
  - `effectiveStatus`: 계산 상태
- 세트 메뉴에 `onSale=true` 를 보내도 구성 단품 중 하나라도 품절이면 응답은 `status=ON_SALE`, `effectiveStatus=SOLD_OUT` 입니다.

#### 4) 메뉴 목록 조회
- `GET /api/v1/owner/menus?page=0&size=10&keyword=세트&type=SET&effectiveStatus=ON_SALE`
- 쿼리 파라미터
  - `page`, `size`
  - `keyword`
  - `type`: 미전송/빈 값이면 전체, `SINGLE` 이면 단품, `SET` 이면 세트
  - `effectiveStatus`: `ON_SALE`, `SOLD_OUT`
- 목록 응답 항목
  - `id`, `name`, `description`, `priceKrw`
  - `imageKey`, `imageMime`, `imageEtag`, `imageStatus`
  - `type`, `status`, `effectiveStatus`
  - `componentCount`

#### 5) 제품 상세 조회
- `GET /api/v1/owner/menus/{menuId}`
- 기존 owner 메뉴 상세 API를 제품 상세 조회 API로 사용합니다.
- 단품은 top-level `description` 에 해당 단품 설명이 내려가고 `components` 는 빈 배열입니다.
- 세트는 top-level `description` 에 해당 세트 설명이 내려가고, `components[{ menuId, name, status, effectiveStatus, quantity }]` 는 현재 구조를 유지합니다.

#### 6) 메뉴 삭제
- `DELETE /api/v1/owner/menus/{menuId}`
- 세트에 참조 중인 단품 삭제는 `409 CONFLICT` 와 `MENU_COMPONENT_IN_USE` 로 차단됩니다.
- 세트 메뉴 자체 삭제는 허용됩니다.

## 노쇼 글 (사장님)
- `POST /api/v1/owner/no-show-posts/batch`
- `GET /api/v1/owner/no-show-posts?page&size&date`
- `GET /api/v1/owner/no-show-posts/{postId}`

## 노쇼 프리셋/예약 등록 (사장님, 스펙 고정)
- `GET /api/v1/owner/no-show-presets/default`
- `PUT /api/v1/owner/no-show-presets/default`
- `POST /api/v1/owner/no-show-post-schedules`
- `GET /api/v1/owner/no-show-post-schedules?page&size&status`
- `GET /api/v1/owner/no-show-post-schedules/{scheduleId}`
- `POST /api/v1/owner/no-show-post-schedules/{scheduleId}/publish-now`
- `DELETE /api/v1/owner/no-show-post-schedules/{scheduleId}`

### 프리셋/예약 정책 (고정)
- 기존 즉시등록 API(`POST /api/v1/owner/no-show-posts/batch`)는 유지한다.
- 프리셋은 현재 매장당 기본 1개(`discountPercent`, `visitAvailableMinutes`, `saleDelayMinutes`)를 사용한다. 향후 복수 프리셋으로 확장 가능해야 한다.
- 예약 등록은 서버가 `requestedAt` 기준으로 `startAt = requestedAt + saleDelayMinutes`를 계산한다.
- 예약 등록은 서버가 `expireAt = startAt + visitAvailableMinutes`를 계산한다.
- 예약 상태가 `QUEUED`일 때만 취소 가능하다.
- `publish-now` 호출 시 예약 대기(`QUEUED`)를 즉시 등록으로 전환한다.
- 예약이 실제 게시되면 기존 노쇼 글 등록 로직(중복 병합/이력 저장/즐겨찾기 알림)과 동일하게 처리한다.

## 노쇼 주문 (사장님)
- `GET /api/v1/owner/orders?page&size&date&status?`
- `GET /api/v1/owner/orders/{orderId}` *(orderId=UUID)*
- `POST /api/v1/owner/orders/{orderId}/complete`

## 매출 (사장님)
- `GET /api/v1/owner/sales?startDate=YYYY.MM.DD&endDate=YYYY.MM.DD&page&size`
- `POST /api/v1/owner/sales/export` (비동기 엑셀 생성 요청)
- `GET /api/v1/owner/sales/export/{jobId}` (폴링 상태 조회/다운로드 URL)
- `POST /api/v1/owner/sales/export/{jobId}/refresh` (다운로드 URL 재발급)

## 매출 엑셀(준비)
- 엑셀 발행 시 **기본=1행=1주문(요약)** 기준. 필요 시 **1행=1아이템(상세)** 옵션 제공.
- 결제 금액은 서버에서 **10원 단위 반올림** 기준으로 검증/저장됨
- `paid_at`이 없으면 공란(미결제/미확정)으로 표기
- 비동기 엑셀 다운로드: **폴링 방식**(jobId → 상태 확인 → presigned URL)
- URL TTL 10분, Redis 캐시 TTL 15분, 파일 보관 7일
- 동일 요청은 **요청 해시(SHA-256)** 기반으로 중복 생성 방지(기존 jobId 반환)

### 기본(1행=1주문, 요약)
| 필드 | 소스 | 비고 |
| --- | --- | --- |
| 주문번호 | `no_show_orders.order_no` | 카드사 대조/고객 응대용 |
| 결제일시 | `no_show_orders.paid_at` | 없으면 공란 |
| 원래 예약시간 | `no_show_orders.visit_time` | 노쇼 분석용 |
| 주문유형 | 상수 `"NO_SHOW"` | 현재 노쇼 판매만 존재 |
| 메뉴명(요약) | `no_show_orders.menu_names` | `name1(qty1), name2(qty2)…` |
| 최종 결제 금액 | `no_show_orders.paid_amount` | 소비자가 실제 낸 금액 |
| 결제 수단 | `no_show_orders.payment_method` | CARD/현금/간편결제 등 |
| 주문 상태 | `no_show_orders.status` | 완료/취소 등 |
| 공급가액 | 계산 | `paid_amount` 기준 10% VAT 역산 |
| 부가세 | 계산 | `paid_amount - 공급가액` |

### 상세(1행=1아이템, 옵션)
| 필드 | 소스 | 비고 |
| --- | --- | --- |
| 주문번호 | `no_show_orders.order_no` | 주문 단위 식별 |
| 결제일시 | `no_show_orders.paid_at` | 없으면 공란 |
| 원래 예약시간 | `no_show_orders.visit_time` | 주문 기준 |
| 주문유형 | 상수 `"NO_SHOW"` |  |
| 메뉴명 | `no_show_order_items.menu_name` | 주문 당시 스냅샷 |
| 수량 | `no_show_order_items.quantity` | 메뉴별 |
| 할인율(%) | `no_show_order_items.discount_percent` | 메뉴별 |
| 원가(단가) | `no_show_order_items.unit_price` | 할인 전 단가 |
| 결제 수단 | `no_show_orders.payment_method` |  |
| 주문 상태 | `no_show_orders.status` |  |

## 소비자 홈/매장
- `GET /api/v1/home` : 오늘 기준 내 주문(최대 3건, PENDING 우선)
- `GET /api/v1/home/stores?lat&lon&page&size`
- `GET /api/v1/stores/{storeId}/no-show-posts?page&size`

## 노쇼 주문 (소비자)
- `POST /api/v1/orders` : 요청 항목(noShowPostId, menuName, qty, originalPrice, discountRate...) -> 생성/결제 성공 시 UUID 주문 ID + orderNo 반환

## 즐겨찾기 (소비자)
- `POST /api/v1/favorites` / `DELETE /api/v1/favorites` (storeId)

## 알림/토큰
- `POST /api/v1/push/tokens` : 토큰 등록(FCM 토큰 중복 시 새 사용자로 재할당)
- `DELETE /api/v1/push/tokens` : 로그아웃 시 토큰 삭제

## 예시 요청 (노쇼 주문 생성)
```json
{
  "storeId": "<UUID>",
  "visitTime": "2025-11-19T15:10:00+09:00",
  "paymentMethod": "CARD",
  "totalAmount": 54600,
  "appliedDiscountAmount": 23400,
  "items": [
    {
      "noShowPostId": 25,
      "menuName": "1인 숙성 모둠회",
      "quantity": 1,
      "originalPrice": 50000,
      "discountRate": 30
    }
  ]
}
```
