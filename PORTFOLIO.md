# Dandi-Onna - 노쇼 재고 손실을 판매 기회로 전환하는 O2O 예약·주문 플랫폼

> - 이 문서는 단디온나 백엔드에서 담당한 구현 범위와 기술적 기여를 정리한 문서입니다.
> - 기준 브랜치 `feat/38-menu-domain-expansion`
> - 문서 브랜치 `docs/dandi-onna-portfolio`

## 1. 프로젝트 개요

단디온나는 음식점 노쇼로 발생한 재고 손실을 줄이기 위해, 사장님이 노쇼 메뉴를 등록하면 소비자가 할인된 가격으로 바로 주문할 수 있도록 만든 O2O 예약·주문 플랫폼입니다.

정식 발표 이후에도 추가 개발을 진행하며, 발표 당시의 AWS/S3 기반 구조를 S3-compatible 스토리지, 로컬 인프라, 운영/성능 측정 체계로 확장했습니다.

| 항목 | 내용 |
|---|---|
| 프로젝트명 | 단디온나 |
| 팀명 | TEAM Ynot? |
| 역할 | 백엔드 단독 개발자 |
| 주요 담당 | 인증/인가, 매장/메뉴, 노쇼 게시글, 예약 게시, 주문, 알림, 매출 조회·엑셀 export, 운영/성능 측정 |
| 기준 브랜치 | `feat/38-menu-domain-expansion` |
| Repository | https://github.com/goorm-ynot/dandi-onna-be |

## 2. 담당 역할 요약

| 영역 | 담당 내용 |
|---|---|
| Auth / Security | Spring Security, JWE Access/Refresh Token, Redis token blacklist, 역할 기반 접근 제어, RateLimit |
| NoShowPost | 노쇼 게시글 즉시 등록, 예약 게시, 판매 가능 수량 관리, 상태 전이, 게시글 히스토리 |
| NoShowOrder | 소비자 주문 생성, 가격/할인율/방문시간 재검증, 잔여 수량 차감, dummy payment 처리 |
| Notification | FCM 알림, Redis Stream 기반 비동기 알림 워커, 알림 이력 저장, DLQ 이동/수동 replay 구조 |
| Export | Redis Stream 기반 매출 엑셀 export worker, Apache POI 기반 엑셀 생성, S3-compatible 업로드, presigned download URL |
| Store / Menu | 사장님 매장 관리, 단품/세트 메뉴, effectiveStatus, 이미지 임시 업로드/확정/소비 흐름 |
| Location | PostGIS 기반 매장 거리 계산, 소비자 홈 매장 조회 |
| Observability / Perf | Prometheus business metric, MDC 기반 JSON 로깅, k6 + Docker 성능 측정 스크립트, seed SQL |
| Operations | bootJar 산출물명 고정, systemd/Nginx 예시, deploy/rollback 스크립트, 운영 env example |

## 3. 주요 구현 내용

### 3.1 인증 / 인가 / 보안

Spring Security 기반 Stateless 보안 구조를 구성했습니다.

주요 구현 내용:

- JWE Access Token / Refresh Token 발급
- Access Token에 사용자 role claim 포함
- Refresh Token에 `typ=refresh` claim 포함
- `kid` 기반 키 식별 및 이전 키 fallback 검증
- JWE 인증 필터를 Security Filter Chain에 등록
- Redis 기반 Access/Refresh Token blacklist
- Lua script 기반 Access/Refresh Token 원자적 폐기
- OWNER / CONSUMER / ADMIN 역할 기반 URL 접근 제어
- Redis INCR/EXPIRE 기반 RateLimit

관련 코드:

- `src/main/java/com/mvp/v1/dandionna/config/Security/SecurityConfig.java`
- `src/main/java/com/mvp/v1/dandionna/config/Security/JweAuthFilter.java`
- `src/main/java/com/mvp/v1/dandionna/config/Security/JweTokenService.java`
- `src/main/java/com/mvp/v1/dandionna/auth/service/AuthService.java`
- `src/main/java/com/mvp/v1/dandionna/auth/service/TokenBlacklistService.java`
- `src/main/java/com/mvp/v1/dandionna/config/Security/RateLimitFilter.java`

### 3.2 노쇼 게시글 등록 / 예약 게시

사장님이 노쇼 메뉴를 즉시 등록하거나 프리셋 기반으로 예약 게시할 수 있는 흐름을 구현했습니다.

주요 구현 내용:

- 사장님 ownerId 기준 매장 조회
- 할인율 30~90% 검증
- 게시 수량 1 이상 검증
- 판매 가능한 메뉴만 노쇼 게시 가능
- 현재 시각 기준 마감시간 범위 검증
- 영업 종료 시간 초과 방지
- 동일 매장/메뉴/마감시간 게시글 잠금 조회
- 기존 게시글 대체 시 `NoShowPostHistory` 저장
- 신규 게시글 생성 시 즐겨찾기 사용자 알림 연동
- 프리셋 기반 예약 게시 생성/취소/즉시 게시
- 예약 게시 처리 상태 전이
- transaction commit 이후 enqueue

관련 코드:

- `src/main/java/com/mvp/v1/dandionna/noshow_post/service/NoShowPostService.java`
- `src/main/java/com/mvp/v1/dandionna/noshow_post/service/NoShowPostScheduleService.java`
- `src/main/java/com/mvp/v1/dandionna/noshow_post/service/PostExpiryScheduler.java`
- `src/main/java/com/mvp/v1/dandionna/noshow_post/repository/NoShowPostRepository.java`
- `src/main/java/com/mvp/v1/dandionna/noshow_post/entity/NoShowPost.java`
- `src/main/java/com/mvp/v1/dandionna/noshow_post/service/NoShowPresetService.java`

### 3.3 소비자 주문 생성 / 잔여 수량 차감

소비자 주문 생성 시 서버에서 가격과 수량을 재검증하고 노쇼 게시글 잔여 수량을 차감하도록 구현했습니다.

주요 구현 내용:

- 주문 항목 null/empty 방어
- store 존재 검증
- 주문 대상 노쇼 게시글을 `PESSIMISTIC_WRITE`로 잠금 조회
- 다른 매장 게시글 포함 방어
- 판매중이 아닌 노쇼 게시글 주문 방어
- 방문 시간 불일치 방어
- 잔여 수량 부족 방어
- 할인율 / 원가 / 결제 금액 / 할인 금액 서버 재검증
- dummy payment 기반 주문 생성
- 주문 항목 생성
- 노쇼 게시글 잔여 수량 차감
- 잔여 수량 0개 시 `sold_out` 상태 전이
- 사장님 알림 enqueue
- 주문 생성 business metric 증가

관련 코드:

- `src/main/java/com/mvp/v1/dandionna/noshow_order/service/NoShowOrderConsumerService.java`
- `src/main/java/com/mvp/v1/dandionna/noshow_order/entity/NoShowOrder.java`
- `src/main/java/com/mvp/v1/dandionna/noshow_order/entity/NoShowOrderItem.java`
- `src/main/java/com/mvp/v1/dandionna/noshow_post/entity/NoShowPost.java`

### 3.4 FCM 알림 / Redis Stream 비동기 워커

알림 요청을 DB에 기록한 뒤 Redis Stream 기반 워커가 비동기로 FCM을 발송하도록 구성했습니다.

주요 구현 내용:

- `notifications` 저장
- `notification_targets` 저장
- Redis Stream `notification:queue` enqueue
- targetId를 payload에 포함해 DB 상태와 연결
- consumer group 기반 worker 처리
- FCM multicast 발송
- 실패 토큰 정리
- 성공/실패 상태 저장
- 실패 attempt 증가
- 최대 실패 횟수 초과 시 DLQ 이동
- DLQ 수동 replay 구조
- 알림 성공/실패 Prometheus counter 증가
- worker graceful shutdown 처리

관련 코드:

- `src/main/java/com/mvp/v1/dandionna/notification/service/NotificationEnqueueService.java`
- `src/main/java/com/mvp/v1/dandionna/notification/producer/NotificationProducer.java`
- `src/main/java/com/mvp/v1/dandionna/notification/worker/NotificationDispatchWorker.java`
- `src/main/java/com/mvp/v1/dandionna/notification/worker/NotificationWorkerRunner.java`
- `src/main/java/com/mvp/v1/dandionna/fcm/service/FcmNotificationService.java`
- `src/main/java/com/mvp/v1/dandionna/notification/service/NotificationHistoryService.java`
- `src/main/java/com/mvp/v1/dandionna/notification/entity/NotificationTarget.java`

### 3.5 매출 엑셀 export

사장님 매출 데이터를 비동기 export job으로 생성하고, S3-compatible storage에 업로드한 뒤 다운로드 URL을 제공했습니다.

주요 구현 내용:

- 기간 조건 `yyyy-MM-dd` 파싱
- storeId / 기간 / includeDetail 기반 requestHash 생성
- Redis lock 기반 중복 export 생성 방지
- 기존 active job 재사용
- ExportJob 상태 관리: `QUEUED`, `PROCESSING`, `DONE`, `FAILED`, `EXPIRED`
- Redis Stream `export:queue` enqueue
- Apache POI `SXSSFWorkbook` 기반 엑셀 생성
- 주문 요약 시트 생성
- 선택 시 주문 상세 시트 생성
- S3-compatible storage 업로드
- fileKey / rowCount / fileExpiresAt 저장
- presigned download URL 발급
- Redis TTL 기반 download URL 캐싱
- 실패 시 최대 3회 재시도 후 FAILED 처리

관련 코드:

- `src/main/java/com/mvp/v1/dandionna/export_job/service/ExportJobService.java`
- `src/main/java/com/mvp/v1/dandionna/export_job/producer/ExportJobProducer.java`
- `src/main/java/com/mvp/v1/dandionna/export_job/worker/ExportJobDispatchWorker.java`
- `src/main/java/com/mvp/v1/dandionna/export_job/worker/ExportJobWorkerRunner.java`
- `src/main/java/com/mvp/v1/dandionna/export_job/entity/ExportJob.java`
- `src/main/java/com/mvp/v1/dandionna/s3/service/UploadService.java`

### 3.6 매장 / 메뉴 / 이미지 업로드

사장님 매장 관리와 메뉴 도메인을 구현하고, 메뉴 이미지 업로드 흐름을 presign → confirm → consume 단계로 분리했습니다.

주요 구현 내용:

- 사장님 매장 생성/조회/수정/삭제
- 사장님 마이페이지 조회
- 단품 / 세트 메뉴 구분
- 세트 메뉴 구성품 검증
- 세트 메뉴에는 단품 메뉴만 포함 가능
- 동일 구성품 중복 방지
- 자기 자신을 세트 구성품으로 등록하는 것 방지
- effectiveStatus 계산
- 구성 단품이 품절이면 세트도 품절 처리
- 품절 전환 시 연결된 open 노쇼 게시글 close 처리
- 메뉴 이미지 presigned download URL 제공
- 메뉴 이미지 임시 업로드 token 발급
- ETag 기반 confirm
- Redis lock 기반 upload token 중복 사용 방지
- transaction commit 이후 temp object 삭제

관련 코드:

- `src/main/java/com/mvp/v1/dandionna/store/service/StoreService.java`
- `src/main/java/com/mvp/v1/dandionna/menu/service/MenuService.java`
- `src/main/java/com/mvp/v1/dandionna/s3/service/MenuImageTempUploadService.java`
- `src/main/java/com/mvp/v1/dandionna/s3/service/UploadService.java`
- `src/main/java/com/mvp/v1/dandionna/s3/dto/UploadTarget.java`

### 3.7 위치 기반 홈 조회

소비자 홈에서 노쇼 게시글이 있는 매장을 사용자 위치 기준 거리순으로 조회하도록 구현했습니다.

주요 구현 내용:

- stores.geom `geometry(Point, 4326)` 생성
- PostGIS extension 활성화
- GIST index 구성
- `ST_DistanceSphere` 기반 사용자 위치와 매장 간 거리 계산
- open 상태이면서 만료되지 않은 노쇼 게시글이 있는 매장만 조회
- 거리 오름차순 정렬
- 매장 이미지 presigned URL 포함
- 소비자 전용 Home API 구성

관련 코드:

- `src/main/java/com/mvp/v1/dandionna/store/repository/StoreRepository.java`
- `src/main/java/com/mvp/v1/dandionna/home/controller/HomeController.java`
- `src/main/java/com/mvp/v1/dandionna/home/service/HomeStoreService.java`
- `src/main/java/com/mvp/v1/dandionna/home/service/HomeService.java`
- `src/main/resources/db/migration/V1__base_schema.sql`

### 3.8 관측성 / 운영 / 성능 측정

운영 가시성과 성능 측정 재현성을 위해 metric, logging, deploy asset, k6 scripts를 정리했습니다.

주요 구현 내용:

- Micrometer Counter 기반 business metric
  - `dandionna.orders.created`
  - `dandionna.notifications.sent`
  - `dandionna.notifications.failed`
  - `dandionna.posts.expired`
- MDC 기반 요청 추적
  - `traceId`
  - `userId`
  - `requestUri`
- prod profile JSON 구조화 로그
- Actuator Prometheus endpoint 노출
- k6 + Docker 기반 성능 측정 래퍼
- 필수 5개 성능 시나리오
- small / medium / large seed SQL
- systemd / Nginx / deploy / rollback 자산 정리

관련 코드/문서:

- `src/main/java/com/mvp/v1/dandionna/config/MetricsConfig.java`
- `src/main/java/com/mvp/v1/dandionna/config/MdcLoggingFilter.java`
- `src/main/resources/logback-spring.xml`
- `src/main/resources/application-perf.yaml`
- `doc/performance-measurement-plan.md`
- `scripts/perf/run-required.sh`
- `scripts/perf/run-k6.sh`
- `scripts/perf/seeds/common.sql`
- `deploy/systemd/`
- `deploy/bin/`
- `deploy/nginx/dandionna.conf`

## 4. 구현 범위 기준

이 문서는 실제 코드와 문서에서 확인 가능한 구현 범위를 기준으로 작성했습니다.

| 영역 | 현재 구현 기준 |
|---|---|
| Storage | AWS SDK S3 API 기반이며, MinIO 등 S3-compatible endpoint에 연결할 수 있도록 환경변수 기반 설정을 정리했습니다. |
| Payment | 현재 주문 생성은 dummy payment 기반으로 처리합니다. 결제 상태, 결제수단, paymentTxId, paidAmount는 도메인 데이터로 관리합니다. |
| Location | 현재 구현은 `ST_DistanceSphere` 기반 거리 계산과 거리순 정렬입니다. 반경 제한은 정책 확장 지점으로 분리할 수 있습니다. |
| Notification Retry | 실패 attempt 증가, requeue, DLQ 이동, 수동 replay 구조가 있습니다. `nextRetryAt` 기반 지연 실행은 별도 확장 지점입니다. |
| Performance | k6 + Docker 기반 측정 절차와 seed SQL을 정리했습니다. 성능 수치 자체는 별도 `artifacts/perf` 결과 파일 기준으로 기록합니다. |
| Monitoring | Prometheus business metric과 MDC JSON 로깅을 구성했습니다. Dashboard/alert는 운영 환경에서 추가 확장할 수 있습니다. |

## 5. 기술 스택

- Java 21
- Spring Boot
- Spring Security
- JWE / JWT
- PostgreSQL / PostGIS
- Redis
- Redis Stream
- Firebase Cloud Messaging
- MinIO / S3-compatible Storage
- AWS SDK S3 API
- Apache POI
- Micrometer / Prometheus
- Logback / LogstashEncoder
- k6
- Docker
- Flyway
- Gradle

## 6. 테스트 / 검증

현재 브랜치 기준으로 `./gradlew test --no-daemon` 실행 결과 전체 테스트가 통과했습니다.

```text
59 tests, 0 failures, 0 errors, 2 skipped
```

인프라 의존 테스트는 `DANDI_RUN_INFRA_TESTS=true` 환경변수가 있을 때만 실행되도록 분리했습니다.

## 7. 이 프로젝트에서 보여줄 수 있는 역량

- 백엔드 단독 개발자로 서비스 핵심 도메인 전체를 구현한 경험
- 인증/인가, 도메인 상태 전이, 동시성 제어, 알림, 파일 export, 이미지 업로드까지 연결한 경험
- Redis Stream을 활용해 알림과 엑셀 export를 비동기 워커로 분리한 경험
- PostGIS 기반 위치 계산과 S3-compatible storage 연동 경험
- Prometheus metric, MDC logging, k6 성능 측정 체계를 통해 운영 관점까지 확장한 경험
- 정식 프로젝트 이후 로컬 인프라와 운영/성능 문서를 정리하며 웹앱 완성도를 높인 경험

## 8. 이력서용 요약

단디온나는 노쇼로 발생한 재고 손실을 즉시 판매 기회로 전환하는 O2O 예약·주문 플랫폼입니다. 백엔드 단독 개발자로 인증/인가, 매장/메뉴, 노쇼 게시글, 예약 게시, 소비자 주문, 알림, 매출 조회·엑셀 export API를 설계·구현했습니다. 특히 JWE 인증, Redis token blacklist, PESSIMISTIC_WRITE 기반 잔여 수량 차감, Redis Stream 알림/엑셀 export worker, S3-compatible 업로드, Prometheus metric, k6 성능 측정 체계까지 백엔드 개발과 운영 관점을 함께 정리했습니다.
