# 작업 로그

> 이 문서는 `doc/` 폴더 내 작업 기록으로, 주요 변경 사항을 타임라인 형태로 정리합니다.

## 2025-11-11

1. **공통 응답/예외 체계 정비**
   - `ApiResponse`/`ErrorCode`/`GlobalExceptionHandler` 를 활용해 컨트롤러들이 동일한 포맷으로 응답하도록 정리하고, `AuthController` 에 `_ping`, `_error` 엔드포인트를 추가해 수동 검증 경로를 마련했습니다.
2. **Flyway 적용 상태 확인**
   - `.env` 에 정의된 RDS(PostgreSQL)에 직접 접속해 `flyway_schema_history` 와 `users`, `stores` 등 테이블이 정상 생성됐음을 확인했습니다.
3. **시큐리티 문서화**
   - `JwtProps`, `SecurityBeans`, `JweTokenService`, `JweAuthFilter`, `SecurityConfig` 에 한글 주석을 보강하고, 요청 흐름을 설명하는 `doc/security-flow.md` + Mermaid 다이어그램을 작성했습니다.
4. **회원가입 도메인 구성**
   - Flyway 스키마를 기반으로 `User` 엔티티, `UserRepository`, `SignUp` DTO/서비스/컨트롤러를 구현하고, `BaseEntity` 공통 추상 클래스를 도입했습니다. `PasswordEncoder` 빈과 `/v1/api/auth/signup` 엔드포인트를 추가했습니다.
5. **응답 모델 정리**
   - 회원가입 응답은 `ApiResponse<SignUpResponse>` 단일 래퍼만 사용하도록 조정했습니다 (`BasicOkResponse` 제거).
6. **회원가입 응답 보안 강화**
   - 가입 성공 시 사용자 정보를 노출하지 않고, 성공 메시지 문자열만 `ApiResponse` 로 반환하도록 변경했습니다 (`SignUpResponse` 제거, 서비스/컨트롤러 정리).
7. **로그인 API 구현**
   - `LoginRequest`/`LoginResponse` DTO, 자격 증명 검증, `JweTokenService` 기반 토큰 발급 로직을 추가했습니다. 잘못된 로그인 정보는 `AUTH_INVALID_CREDENTIALS` 로 처리합니다.
8. **토큰 블랙리스트 인프라**
   - `TokenBlacklistService` 로 Redis 기반 Access/Refresh 블랙리스트를 관리하고, `JweAuthFilter` 가 인증 전에 블랙리스트를 조회하도록 반영했습니다. `security-flow.md` 도 이에 맞게 갱신했습니다.
9. **로그아웃 API 구현**
   - `LogoutRequest` DTO, Access 헤더 추출 로직, `AuthService.logout` 을 추가해 전달받은 Access/Refresh 토큰을 모두 블랙리스트에 등록하도록 했습니다.
10. **토큰 재발급 API 구현**
    - `RefreshTokenRequest` DTO 와 `AuthService.refresh` 를 통해 블랙리스트 여부 검사 → 리프레시 토큰 검증 → 신규 Access/Refresh 발급 흐름을 완성했습니다.
11. **AuthService 단위 테스트**
    - 로그인/회원가입/로그아웃/토큰 재발급 시나리오를 `AuthServiceTest` 에서 Mockito 기반으로 검증했습니다.
12. **이슈 템플릿 & CI**
    - `.github/ISSUE_TEMPLATE/task.md` 로 공통 작업 이슈 포맷을 추가하고, GitHub Actions `ci` 워크플로우에서 핵심 단위 테스트를 자동 실행하도록 구성했습니다.
13. **PostgreSQL Enum 매핑 보완**
     - `User.role` 필드에 `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` 과 `columnDefinition = "user_role"` 을 적용해 Postgres enum 타입과 JPA 매핑이 정확히 일치하도록 수정했습니다.
14. **Push Token 불필요 컬럼 제거**
     - Flyway `V2__alter_push_tokens_drop_unused_columns.sql` 로 `push_tokens` 테이블의 `expires_at`, `webpush_endpoint`, `is_revoked` 를 삭제해 유지 필드만 남겼습니다.
15. **사장님 테스트 데이터 시드**
     - `V4__seed_owner_test_data.sql` 에서 CEO1~CEO22 계정, 점주 프로필, 매장(영업시간 포함)을 삽입하고, 전화번호 컬럼을 BIGINT 로 변환한 뒤 하이픈을 제거한 숫자값을 저장했습니다.
16. **푸시 토큰 등록 API**
     - `fcm` 패키지에 엔티티/레포지토리/서비스/컨트롤러를 추가하고, `POST /v1/api/push/tokens` 로 FCM 토큰을 등록·갱신하도록 구현했습니다. `PushTokenServiceTest` 로 단위 테스트도 포함했습니다.
17. **보안/토큰 문서 보강**
     - `doc/security-flow.md` 에 리프레시 토큰을 DTO 로 받는 이유와 블랙리스트 처리, 푸시 토큰 등록 특이사항을 기록했습니다.
18. **매장 이미지 컬럼 추가**
     - `V5__add_store_image_columns.sql` 로 `stores` 테이블에 `image_key/mime/etag/image_status` 를 추가해 매장 이미지도 S3 메타데이터를 저장할 수 있게 했습니다.

## 2025-11-14

1. **전화번호 TEXT 전환 & 새 시드 정리**
   - `V6__reset_store_owner_phone_to_text.sql` 로 `owner_profiles.phone`, `stores.phone` 을 TEXT 로 되돌리고 하이픈 포함 입력을 허용했습니다.
   - `V7__seed_owner_store_phone_text.sql` 로 CEO1~CEO22 테스트 계정/프로필/매장 데이터를 하이픈 포함 전화번호와 새 bcrypt 해시로 갱신했습니다.

2. **매장 CRUD · 이미지 플로우 안정화**
   - `StoreController/Service/DTO` 를 정비하고 설명(description) + 이미지 필드를 포함한 등록/수정/조회/삭제 API 를 완성했습니다.
   - `UploadService` / `StoreUploadController` 에 Presign/Confirm/View 를 추가하고, 정적 툴(`static/store-admin.html`)에서 Access Token 기반으로 매장 생성·수정·조회, 이미지 업로드·미리보기를 한 번에 수행할 수 있도록 했습니다.

3. **SecurityUtils 도입**
   - 반복되던 `currentUserId()` 코드를 `common/service/SecurityUtils` 로 이관하고, Store·Upload·NoShow 등 인증이 필요한 컨트롤러들이 공통 유틸을 사용하도록 변경했습니다.

4. **노쇼 게시 도메인 구축**
   - `noshow_post` 패키지에 엔티티/레포지토리/서비스/컨트롤러를 추가하고, `POST /owner/no-show-posts/batch` API 를 구현했습니다.
   - 메뉴 정보를 DB에서 조회하도록 `menu/entity`, `menu/repository` 를 도입했고, 할인율·TTL·수량·중복 메뉴 검증과 영업 종료시간 제한 로직을 서비스에 반영했습니다.

5. **노쇼 만료시간/상태 개선**
   - 요청 DTO가 만료 시간을 `OffsetDateTime expireAt` 으로 받도록 바꾸고, 서비스에서 현재 시각 기준 10분 단위 올림·0~300분 범위·영업 종료시간을 모두 검증합니다.
   - `NoShowPostStatus` 주석을 보강하고, `open/sold_out/expired/closed` 중심으로 상태를 단순화했습니다.

6. **노쇼 덮어쓰기 & 히스토리**
   - `V9__add_no_show_post_history.sql` 로 `(store_id, menu_id, expire_at)` UNIQUE 제약과 `no_show_post_history` 테이블을 추가하고, 덮어쓰기 전에 히스토리를 남기도록 했습니다.
   - `NoShowPostService` 는 기존 글을 `findForUpdate` 로 잠근 뒤 히스토리에 복사하고, 잔여 수량 + 신규 수량을 합산해 동일 행을 덮어쓰는 로직으로 개편했습니다.
   - `V10__rename_no_show_price_columns.sql` 로 가격 컬럼을 `original_unit_price` / `discounted_unit_price` 로 명확히 했습니다.

7. **노쇼 서비스 단위 테스트 & CI 실행**
   - `NoShowPostServiceTest` 를 작성해 “신규 게시글 생성”과 “기존 글 덮어쓰기/히스토리 기록” 시나리오를 Mockito 기반으로 검증했습니다.
   - `./gradlew test` 를 실행해 전체 테스트가 정상 통과함을 확인했습니다.

8. **노쇼 API 응답 통일**
   - `NoShowPostController` 는 `ApiResponse<String>` 으로 성공/실패 여부와 메시지를 반환하도록 수정했습니다.

9. **노쇼 조회/상세 API 보완**
   - 노쇼 목록 응답 구조를 단순화(`data.posts[] + pagination`)하고, 방문시간 내림차순 상위 N개만 반환하도록 변경했습니다.
   - `GET /owner/no-show-posts/{postId}` 상세 API 를 추가해 노쇼 글 단건의 메뉴명/마감시간/남은 수량/정상단가/할인율을 내려줍니다.
   - 관련 DTO/서비스/테스트를 모두 업데이트하고 `./gradlew test` 로 정상 동작을 검증했습니다.

앞으로도 작업 시 본 로그를 계속 갱신해 변경 이력을 추적합니다.
앞으로도 작업 시 본 문서를 계속 갱신해 누락되는 변경 사항이 없도록 관리합니다.

## 2025-11-16

1. **메뉴 이미지 열람 API 추가**
   - `MenuUploadController` 에 `GET /api/v1/menus/{menuId}/uploads/view` 엔드포인트를 추가해, 사장 인증 후 메뉴의 `imageKey` 를 확인하고 Presign GET URL 을 내려주도록 구현했습니다.
   - 메뉴 이미지가 없는 경우 `BUSINESS NOT_FOUND` 예외를 던져 클라이언트가 상태를 명확히 알 수 있도록 했습니다.
2. **관리자 페이지 메뉴 섹션 UX 개선**
   - `static/store-admin.html` 에 메뉴 목록 카드 레이아웃, 이미지 프리뷰, 가격 포맷팅, 설명 영역 등을 추가해 목록을 직관적으로 확인할 수 있게 했습니다.
   - 메뉴 목록 조회 시 새로 만든 view API 를 호출해 각 카드에 이미지를 표시하고, 단순 JSON 응답 외에도 시각적으로 상태를 파악할 수 있도록 보강했습니다.
3. **매장 API 토큰 기반화**
   - `StoreController`, `StoreUploadController`, `StoreService` 를 리팩터링해 더 이상 `storeId` 경로 변수를 요구하지 않고, Access Token 의 사용자 ID 로 1:1 매장을 찾도록 바꿨습니다.
   - 이에 맞춰 정적 관리자 페이지의 매장 조회/수정/이미지 업로드 섹션에서 `storeId` 입력을 제거하고, `/api/v1/stores` 및 `/api/v1/stores/uploads` 엔드포인트를 직접 호출하도록 수정했습니다.
4. **노쇼 테스트 탭 & KST 만료시간**
    - 관리자 페이지에 “노쇼 글 생성” 탭을 추가해 여러 메뉴/수량/할인율/만료시간 입력 후 `/owner/no-show-posts/batch` API 를 바로 호출할 수 있도록 했습니다.
    - 노쇼 만료시간을 프런트에서 한국 시간(+09:00) ISO 문자열로 변환해 전달하도록 수정했고, `NoShowPost` 엔티티의 할인가격 컬럼을 `discounted_unit_price` 로 맞춰 DB 스키마와 일치시켰습니다.
5. **노쇼 히스토리 Enum 매핑 수정**
    - `NoShowPostHistory` 의 `status` 필드에 `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` + `columnDefinition = "no_show_post_status"` 를 지정해 Postgres enum 타입과 일치하도록 했습니다. 이로 인해 동일 메뉴/시간 덮어쓰기 시 히스토리 저장이 실패하던 문제를 해결했습니다.
6. **노쇼 주문 스키마 초안**
    - `V12__add_no_show_orders.sql` 로 `no_show_orders`, `no_show_order_items` 테이블과 인덱스를 추가해 주문/아이템 구조를 준비했습니다.
    - JPA 엔티티(`NoShowOrder`, `NoShowOrderItem`, `NoShowOrderStatus`)와 레포지토리를 생성해 앞으로의 주문 API 구현 기반을 마련했습니다.
7. **노쇼 주문 결제 필드 확장**
    - `V13__extend_no_show_orders_payment.sql` 로 결제 상태 enum(`payment_status`)과 결제 수단/거래 ID/금액/타임스탬프 컬럼을 `no_show_orders`에 추가했습니다.
    - `NoShowPaymentStatus` enum과 `NoShowOrder` 엔티티 필드를 보강해 기본 결제 정보를 저장·조회할 수 있게 했습니다.
8. **노쇼 주문 아이템 스냅샷 개선**
    - `V14__add_menu_name_to_no_show_order_items.sql` 로 `menu_name` 컬럼을 추가해 주문 시점의 메뉴명을 저장하도록 했습니다.
    - `NoShowOrderItem`, 상세 응답을 수정해 재조회 시 메뉴명·수량·단가·할인율·방문시간을 그대로 보여주도록 했습니다.
9. **노쇼 주문 요약용 menu_names 추가**
    - `V15__add_menu_names_to_no_show_orders.sql` 로 주문 헤더에 `menu_names` 텍스트 컬럼을 추가해 “name(quantity)” 목록을 저장할 수 있게 했습니다.
10. **소비자 홈 API**
     - `HomeResponse`/`HomeService`/`HomeController` 를 추가해 `/api/v1/home` 호출 시 “오늘 기준 내가 주문한 가게” 목록(최대 3건)을 반환하도록 구현했습니다.
     - `NoShowOrderRepository` 에 소비자 ID 기반 조회 메서드가 추가되었고, `consumer_profiles` 정보를 JPA 레포지토리로 조회해 전화번호를 함께 내려줍니다.
11. **소비자용 관리자 탭 개선**
     - `static/store-admin.html` 소비자 탭에 별도 API Base URL + 토큰 입력 섹션을 추가하고, 저장 시 사장/소비자 주소가 동기화되도록 수정했습니다.
     - `/api/v1/home`, `/api/v1/home/stores` 호출은 소비자 토큰과 Base URL을 사용하도록 분리해 잘못된 호스트로 호출되며 `Failed to fetch` 가 나는 문제를 방지했습니다.
12. **소비자 테스트 계정 시드**
     - `V16__seed_consumer_test_account.sql` 로 `Customer1 / 111111` 계정을 CONSUMER 역할로 생성하고, 기본 이름/전화번호가 담긴 `consumer_profiles` 를 같이 upsert 하도록 했습니다.
     - 관리자 페이지 소비자 탭에서 바로 사용할 수 있는 토큰을 발급받기 위한 기반 데이터로 활용됩니다.
13. **매장별 노쇼 글 조회 API**
     - `StoreConsumerController` + `StoreConsumerService` 를 추가해 `/api/v1/stores/{storeId}/no-show-posts` 호출 시 소비자 토큰으로 해당 매장의 활성 노쇼 글을 마감 시간순으로 내려주도록 구현했습니다.
     - `StoreNoShowPostsResponse` DTO에는 매장 정보, 노쇼 글(메뉴명/설명, 원가/할인가, 남은 수량, 이미지 URL)과 페이지 정보가 포함됩니다. 관리자 페이지 소비자 탭에도 테스트 섹션을 추가했습니다.
14. **즐겨찾기(Favorites) API**
     - `Favorite` 엔티티/리포지토리/서비스/컨트롤러를 추가해 소비자가 `/api/v1/favorites` (POST/DELETE) 로 즐겨찾기를 추가·취소할 수 있도록 구현했습니다. 사전 중복 검사로 이미 즐겨찾기된 경우에도 안전하게 처리합니다.
     - 즐겨찾기 요청 결과는 상태 메시지를 포함해 반환하며, 소비자 탭에 테스트 폼을 추가했습니다. 또한 알림 트리거를 위해 매장별 즐겨찾기 소비자 목록을 조회하는 메서드를 서비스에 준비했습니다.
15. **소비자 주문 API**
     - `NoShowOrderConsumerService`/`NoShowOrderConsumerController` 와 `NoShowOrderCreateRequest/Response` 를 통해 `/api/v1/orders` 에서 소비자가 노쇼 글을 결제·주문할 수 있도록 구현했습니다.
     - 노쇼 글/메뉴/금액을 트랜잭션 내에서 검증하고, 재고를 잠그기 위한 PESSIMISTIC WRITE 쿼리를 추가했습니다. 주문 생성 시 메뉴명·가격·할인율 등을 스냅샷으로 저장하며, 관리자 페이지에도 테스트 섹션을 추가했습니다.
     - 소비자 클라이언트가 `menuId` 를 알 수 없는 경우를 고려해 주문 항목에서 menuId 입력을 없애고, 서버가 노쇼 글 정보를 기반으로 메뉴를 확인하도록 개선했습니다.
     - 방문 시간 비교 시 타임존 오프셋 차이로 실패하지 않도록 `OffsetDateTime` 의 `Instant` 기준으로 검증하도록 수정했습니다.
     - `total_price` 컬럼에는 원가 총합, `paid_amount` 에는 할인가 총합이 저장되도록 주문 생성 로직을 조정해 `/api/v1/home` 응답이 원/실 결제 금액을 명확히 구분하도록 했습니다.
     - 사장님 노쇼 글 목록 조회 시 `visitTime` 기준 오름차순으로 정렬되도록 서비스 정렬 조건을 조정했습니다.
     - FirebaseMessaging 을 이용한 FCM 서비스를 추가하고, 주문 생성 시 사장님 계정의 모든 기기로 `"[노쇼 주문] {소비자명} 님이 주문했어요"` 알림(주문번호/메뉴/결제금액/방문시간/연락처, 딥링크 `/seller/order`)을 전송하도록 연동했습니다.
16. **토큰 리프레시 정책 변경**
    - `/api/v1/auth/token/refresh` 호출 시 더 이상 기존 리프레시 토큰을 블랙리스트에 넣지 않고, Access Token만 새로 발급하도록 `AuthService.refresh` 를 수정했습니다.
    - 로그아웃 시 등록된 블랙리스트 값은 그대로 유지하며, 리프레시 토큰은 만료 시점까지 재사용 가능하게 정책을 조정했습니다.
17. **FCM 토큰 관리 강화**
    - 동일 디바이스에서 계정을 전환하면 기존 토큰을 새 사용자에게 재할당하도록 `PushTokenService.register` 로직을 손보고, 토큰 삭제용 요청(`DELETE /api/v1/push/tokens`)을 추가했습니다.
    - FCM 전송 실패 시 `UNREGISTERED/INVALID_*` 코드면 서버 DB에서 해당 토큰을 즉시 삭제해 stale 토큰으로 인한 누락을 줄입니다.
18. **즐겨찾기 노쇼 알림**
    - `FavoriteNotificationService` 를 신설해 매장 즐겨찾기 소비자들에게 노쇼 글 등록 소식을 FCM으로 전송합니다. 두 가지 랜덤 템플릿을 사용하며 매장/메뉴/할인 정보와 딥링크를 포함합니다.
    - `NoShowPostService.createBatch` 에서 신규 노쇼 글 생성 시 자동으로 호출되어 즐겨찾기한 모든 유저가 알림을 받습니다.
19. **알림 로깅/재시도 인프라 준비**
    - `notifications`/`notification_targets` 테이블을 확장해 카테고리, 우선순위, 재시도 카운트, 에러 기록, 인덱스를 추가했습니다(`V17__extend_notifications_tables.sql`).
    - Redis Stream 기반 전송 워커 스켈레톤을 추가하고(지수 백오프 5→10→15초, 최대 3회 재시도), 멀티캐스트 FCM 전송을 사용하는 `NotificationDispatchWorker` 를 작성했습니다(현재 DB 상태 업데이트는 TODO).
    - FCM 전송은 data-only payload(`title/body/deeplink` 포함) 멀티캐스트로 전환해 클라이언트 수동 알림 처리 방식을 지원합니다.
20. **소비자 찜 여부 표시**
    - 매장 노쇼 글 조회 응답에 `favorited` 필드를 추가해 현재 소비자가 해당 매장을 즐겨찾기했는지 Boolean으로 반환합니다.

### TODO / 추후 검토
- 노쇼 글 만료 처리 자동화  
  * 현재 `no_show_posts.status` 는 생성/덮어쓰기 시 `open` 으로만 설정되며, `expire_at` 이 지나도 자동으로 `expired` 로 전환되지 않는다.  
  * `NoShowPostService.listPosts()` 도 만료된 글을 필터링하지 않으므로, 만료 후에도 목록/상세 응답에 그대로 포함된다.  
  * 향후 스케줄러 또는 배치 작업으로 `expire_at < now` 인 글을 주기적으로 `expired` 로 변경하고, 필요 시 `no_show_post_history` 에 기록하는 로직을 추가해야 한다.  
  * 조회 API에 상태 필터나 `status` 필드 포함 여부도 함께 검토한다.
