# 예외 설계 (현 시점 기준)

- **형식**: `ApiResponse` 래퍼로 `{ success, code, message, data }` 반환
- **공통 카테고리**
  - AUTH_INVALID_TOKEN / AUTH_INVALID_CREDENTIALS / AUTH_FORBIDDEN_ROLE
  - BAD_REQUEST (입력 검증 실패, 날짜 형식 오류, 금액 불일치 등)
  - NOT_FOUND (매장/메뉴/노쇼글/주문/사용자 없음)
  - BUSINESS (재고 부족, 상태 불일치, 만료 등 비즈니스 규칙 위반)
  - INTERNAL_ERROR (예상치 못한 오류)
- **대표 규칙**
  - 인증/권한: JWT 파싱 실패, 블랙리스트 토큰 → AUTH_INVALID_TOKEN
  - 역할 불일치 → AUTH_FORBIDDEN_ROLE
  - 날짜 파싱 실패(YYYY.MM.DD 아님) → BAD_REQUEST
  - 주문 검증: 금액·할인 불일치, 재고 부족, 만료 시간 불일치 → BAD_REQUEST/BUSINESS
  - 리소스 없음: 존재하지 않는 store/menu/post/order/user → NOT_FOUND
- **FCM/토큰**
  - UNREGISTERED/INVALID 토큰은 전송 실패 시 DB에서 제거(로그 기록)
- **로그 거점**
  - GlobalExceptionHandler에서 예외 매핑 및 상태 코드 변환
