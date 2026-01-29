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

## 노쇼 주문 (사장님)
- `GET /api/v1/owner/orders?page&size&date&status?`
- `GET /api/v1/owner/orders/{orderId}` *(orderId=UUID)*
- `POST /api/v1/owner/orders/{orderId}/complete`

## 매출 (사장님)
- `GET /api/v1/owner/sales?startDate=YYYY.MM.DD&endDate=YYYY.MM.DD&page&size`

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
