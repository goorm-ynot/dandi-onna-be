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
- `POST /api/v1/owner/menus` / `PATCH /api/v1/owner/menus/{id}` / `DELETE /api/v1/owner/menus/{id}` / `GET {id}` / `GET ?page&size`
- 업로드: `POST /api/v1/stores/uploads/presign|confirm|view`, `POST /api/v1/menus/{id}/uploads/presign|confirm|view`

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
