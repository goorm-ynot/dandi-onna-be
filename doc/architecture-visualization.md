# 단디온나 백엔드 아키텍처 시각화

> 본 문서는 현재(AS-IS) 아키텍처와 개선 목표(TO-BE) 아키텍처를 Mermaid 다이어그램으로 시각화합니다.

---

## 1. 시스템 전체 구조 (AS-IS)

```mermaid
flowchart TB
    subgraph Clients["🖥️ 클라이언트"]
        OwnerApp["사장님 앱/웹"]
        ConsumerApp["소비자 앱/웹"]
    end

    subgraph LB["로드밸런서 / 리버스 프록시"]
        Gateway["(미구현)"]
    end

    subgraph SpringBoot["Spring Boot 애플리케이션"]
        subgraph Security["보안 계층"]
            JweAuthFilter["JweAuthFilter\n(Bearer 토큰 추출)"]
            SecurityConfig["SecurityConfig\n(필터 체인)"]
            JweTokenService["JweTokenService\n(토큰 발급/검증)"]
            TokenBlacklist["TokenBlacklistService\n(Redis 블랙리스트)"]
        end

        subgraph Controllers["컨트롤러 계층"]
            AuthCtrl["AuthController\n/api/v1/auth/**"]
            StoreCtrl["StoreController\n/api/v1/stores/**"]
            StoreConsCtrl["StoreConsumerController\n/api/v1/stores/{id}/no-show-posts"]
            MenuCtrl["MenuController\n/api/v1/owner/menus/**"]
            NoShowPostCtrl["NoShowPostController\n/api/v1/owner/no-show-posts/**"]
            NoShowOrderCtrl["NoShowOrderController\n/api/v1/owner/orders/**"]
            ConsumerOrderCtrl["NoShowOrderConsumerController\n/api/v1/orders"]
            SalesCtrl["OwnerSalesController\n/api/v1/owner/sales/**"]
            SalesExportCtrl["OwnerSalesExportController\n/api/v1/owner/sales/export/**"]
            HomeCtrl["HomeController\n/api/v1/home/**"]
            FavCtrl["FavoriteController\n/api/v1/favorites"]
            PushCtrl["PushTokenController\n/api/v1/push/tokens"]
            UploadCtrls["StoreUploadController\n/api/v1/stores/uploads/**\nMenuTempUploadController\n/api/v1/owner/menu-images/temp/**"]
        end

        subgraph Services["서비스 계층"]
            AuthSvc["AuthService"]
            StoreSvc["StoreService"]
            StoreConsSvc["StoreConsumerService"]
            MenuSvc["MenuService"]
            MenuPermSvc["MenuPermissionService"]
            NoShowPostSvc["NoShowPostService"]
            NoShowOrderSvc["NoShowOrderService"]
            ConsumerOrderSvc["NoShowOrderConsumerService"]
            SalesSvc["OwnerSalesService"]
            ExportJobSvc["ExportJobService"]
            HomeSvc["HomeService"]
            HomeStoreSvc["HomeStoreService"]
            FavSvc["FavoriteService"]
            FavNotiSvc["FavoriteNotificationService"]
            PushTokenSvc["PushTokenService"]
            FcmSvc["FcmNotificationService"]
            UploadSvc["UploadService"]
            NotiEnqSvc["NotificationEnqueueService"]
        end

        subgraph AsyncWorkers["비동기 워커"]
            NotiWorker["NotificationDispatchWorker\n(Redis Stream 소비)"]
            NotiRunner["NotificationWorkerRunner"]
            ExportWorker["ExportJobDispatchWorker\n(Redis Stream 소비)"]
            ExportRunner["ExportJobWorkerRunner"]
        end

        subgraph Repositories["리포지토리 계층"]
            UserRepo["UserRepository"]
            StoreRepo["StoreRepository"]
            MenuRepo["MenuRepository"]
            NoShowPostRepo["NoShowPostRepository"]
            NoShowPostHistRepo["NoShowPostHistoryRepository"]
            NoShowOrderRepo["NoShowOrderRepository"]
            NoShowOrderItemRepo["NoShowOrderItemRepository"]
            FavRepo["FavoriteRepository"]
            PushTokenRepo["PushTokenRepository"]
            NotiRepo["NotificationRepository"]
            NotiTargetRepo["NotificationTargetRepository"]
            ExportJobRepo["ExportJobRepository"]
            ConsumerProfileRepo["ConsumerProfileRepository"]
            OwnerProfileRepo["OwnerProfileRepository"]
        end

        subgraph Common["공통 모듈"]
            ApiResponse["ApiResponse / ErrorCode"]
            BaseEntity["BaseEntity (audit)"]
            GlobalExHandler["GlobalExceptionHandler"]
            SecurityUtils["SecurityUtils"]
            BizException["BusinessException"]
        end
    end

    subgraph Infra["인프라"]
        DB[("PostgreSQL\n+ PostGIS")]
        Redis[("Redis\nBlacklist + Streams")]
        S3[("S3 / MinIO\nPresigned URL")]
        FCM[("Firebase Cloud\nMessaging")]
    end

    Clients -->|"HTTPS + JWT"| SecurityConfig
    SecurityConfig --> JweAuthFilter
    JweAuthFilter --> TokenBlacklist
    JweAuthFilter --> JweTokenService
    JweAuthFilter --> Controllers

    Controllers --> Services
    Services --> Repositories
    Repositories --> DB

    TokenBlacklist --> Redis
    NotiEnqSvc --> Redis
    ExportJobSvc --> Redis

    Redis --> NotiWorker
    Redis --> ExportWorker
    NotiWorker --> FCM
    ExportWorker --> S3

    UploadSvc --> S3
    FcmSvc --> FCM

    FCM -.->|"Push"| Clients
```

---

## 2. 패키지 구조 & 의존 관계 (AS-IS)

```mermaid
flowchart LR
    subgraph config["config"]
        Security["Security/\nSecurityConfig\nJweAuthFilter\nJweTokenService\nJwtProps\nSecurityBeans"]
        Aws["Aws/\nAwsConfig\nMinioHttpClientConfig"]
        Fcm["Fcm/\nFirebaseConfig"]
    end

    subgraph auth["auth"]
        AuthCtrl["controller/\nAuthController"]
        AuthSvc["service/\nAuthService\nTokenBlacklistService"]
        AuthEntity["entity/\nUser, UserRole"]
        AuthRepo["repository/\nUserRepository"]
        AuthDto["dto/\nLogin*, Logout*\nRefresh*, SignUp*"]
    end

    subgraph store["store"]
        StoreCtrl2["controller/\nStoreController\nStoreConsumerController"]
        StoreSvc2["service/\nStoreService\nStoreConsumerService"]
        StoreEntity["entity/\nStore, ImageStatus"]
        StoreRepo2["repository/\nStoreRepository"]
    end

    subgraph menu["menu"]
        MenuCtrl2["controller/\nMenuController"]
        MenuSvc2["service/\nMenuService\nMenuPermissionService"]
        MenuEntity["entity/\nMenu"]
        MenuRepo2["repository/\nMenuRepository"]
    end

    subgraph noshow_post["noshow_post"]
        NspCtrl["controller/\nNoShowPostController"]
        NspSvc["service/\nNoShowPostService"]
        NspEntity["entity/\nNoShowPost\nNoShowPostHistory\nNoShowPostStatus"]
        NspRepo["repository/\nNoShowPost*Repository"]
    end

    subgraph noshow_order["noshow_order"]
        NsoCtrl["controller/\nNoShowOrderController\nConsumerController\nSalesController\nSalesExportController"]
        NsoSvc["service/\nNoShowOrderService\nConsumerService\nOwnerSalesService"]
        NsoEntity["entity/\nNoShowOrder\nNoShowOrderItem\nNoShowOrderStatus\nNoShowPaymentStatus"]
        NsoRepo["repository/\nNoShowOrder*Repository"]
    end

    subgraph favorite["favorite"]
        FavCtrl2["controller/\nFavoriteController"]
        FavSvc2["service/\nFavoriteService\nFavoriteNotificationService"]
        FavEntity["entity/\nFavorite, FavoriteId"]
        FavRepo2["repository/\nFavoriteRepository"]
    end

    subgraph fcm_pkg["fcm"]
        FcmCtrl["controller/\nPushTokenController"]
        FcmSvc2["service/\nPushTokenService\nFcmNotificationService"]
        FcmEntity["entity/\nPushToken, Platform"]
        FcmRepo["repository/\nPushTokenRepository"]
    end

    subgraph home_pkg["home"]
        HomeCtrl2["controller/\nHomeController"]
        HomeSvc2["service/\nHomeService\nHomeStoreService"]
    end

    subgraph s3_pkg["s3"]
        S3Ctrl["controller/\nStoreUploadController\nMenuTempUploadController"]
        S3Svc["service/\nUploadService\nMenuImageTempUploadService"]
    end

    subgraph notification["notification"]
        NotiSvc["service/\nNotificationEnqueueService"]
        NotiWorker2["worker/\nDispatchWorker\nWorkerRunner"]
        NotiProducer["producer/\nNotificationProducer"]
        NotiEntity["entity/\nNotification\nNotificationTarget"]
        NotiRepos["repository/\nNotification*Repository"]
    end

    subgraph export_job["export_job"]
        ExportSvc["service/\nExportJobService"]
        ExportWorker2["worker/\nDispatchWorker\nWorkerRunner"]
        ExportProducer["producer/\nExportJobProducer"]
        ExportEntity["entity/\nExportJob\nExportJobStatus"]
        ExportRepos["repository/\nExportJobRepository"]
    end

    %% 핵심 의존 흐름
    auth -->|"토큰 발급"| Security
    NspSvc -->|"알림 트리거"| FavSvc2
    FavSvc2 -->|"FCM 전송"| FcmSvc2
    NsoSvc -->|"주문 알림"| FcmSvc2
    NotiSvc -->|"Redis Stream"| NotiWorker2
    NotiWorker2 -->|"FCM"| FcmSvc2
    ExportSvc -->|"Redis Stream"| ExportWorker2
    HomeSvc2 -->|"매장 조회"| store
    HomeSvc2 -->|"주문 조회"| noshow_order
    S3Ctrl -->|"이미지"| store
    S3Ctrl -->|"이미지"| menu
```

---

## 3. 데이터 흐름 (핵심 시나리오)

### 3-1. 인증 흐름

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant F as JweAuthFilter
    participant BL as TokenBlacklistService
    participant TS as JweTokenService
    participant SC as SecurityContext
    participant Ctrl as Controller

    C->>F: HTTP + Authorization: Bearer <JWE>
    F->>BL: isBlacklisted(token)?
    alt 블랙리스트
        BL-->>F: true
        F-->>C: 인증 없이 체인 진행 → 401
    else 유효
        BL-->>F: false
        F->>TS: parseClaims(token)
        TS-->>F: Claims(userId, role, exp)
        F->>SC: setAuthentication(token)
        F->>Ctrl: chain.doFilter()
        Ctrl->>SC: getAuthentication()
        Ctrl-->>C: 200 응답
    end
```

### 3-2. 노쇼 주문 & 알림 흐름

```mermaid
sequenceDiagram
    autonumber
    participant Consumer as 소비자 앱
    participant API as OrderConsumerController
    participant Svc as ConsumerOrderService
    participant DB as PostgreSQL
    participant FCM as FcmNotificationService
    participant Owner as 사장님 기기

    Consumer->>API: POST /api/v1/orders
    API->>Svc: createOrder(request)
    Svc->>DB: 노쇼글 조회 + 비관적 잠금
    Svc->>DB: 재고 차감 & 주문 생성
    Svc->>DB: 주문 아이템 스냅샷 저장
    Svc->>FCM: 사장님 기기로 푸시 알림 전송
    FCM->>Owner: "[노쇼 주문] 00님이 주문했어요"
    Svc-->>API: OrderCreateResponse
    API-->>Consumer: 200 + 주문번호/UUID
```

### 3-3. 이미지 업로드 흐름

```mermaid
sequenceDiagram
    autonumber
    participant Client as 클라이언트
    participant API as UploadController
    participant Svc as UploadService
    participant S3 as S3/MinIO

    Client->>API: POST /uploads/presign (fileName, contentType)
    API->>Svc: presign(target, referenceId, request)
    Svc->>S3: S3Presigner.presignPutObject()
    S3-->>Svc: Presigned PUT URL
    Svc-->>API: {url, key, expiresInSeconds}
    API-->>Client: Presigned URL 응답

    Client->>S3: PUT 파일 업로드 (Presigned URL)
    S3-->>Client: 200 + ETag

    Client->>API: POST /uploads/confirm (key, etag)
    API->>Svc: confirm(target, referenceId, key, etag)
    Svc->>S3: headObject() ETag/크기 검증
    S3-->>Svc: 메타데이터
    Svc-->>API: S3Metadata
    API-->>Client: 확인 완료
```

### 3-4. 비동기 알림 워커 흐름

```mermaid
sequenceDiagram
    autonumber
    participant Svc as NotificationEnqueueService
    participant DB as PostgreSQL
    participant Redis as Redis Stream
    participant Worker as NotificationDispatchWorker
    participant FCM as Firebase CM

    Svc->>DB: Notification + NotificationTarget 저장
    Svc->>Redis: XADD notification-stream
    Redis-->>Worker: XREADGROUP (폴링)
    Worker->>DB: 타겟 조회 (QUEUED)
    Worker->>FCM: sendEachForMulticast()
    alt 성공
        Worker->>DB: status = SENT
    else 실패 (재시도 가능)
        Worker->>DB: retry_count++, status = QUEUED
        Note over Worker: 지수 백오프 (5s → 10s → 15s, 최대 3회)
    else UNREGISTERED
        Worker->>DB: 토큰 삭제
    end
    Worker->>Redis: XACK
```

### 3-5. 매출 엑셀 Export 흐름

```mermaid
sequenceDiagram
    autonumber
    participant Owner as 사장님
    participant API as SalesExportController
    participant Svc as ExportJobService
    participant DB as PostgreSQL
    participant Redis as Redis Stream
    participant Worker as ExportJobDispatchWorker
    participant S3 as S3/MinIO

    Owner->>API: POST /owner/sales/export
    API->>Svc: requestExport(storeId, dateRange)
    Svc->>DB: request_hash 중복 확인
    alt 기존 작업 존재
        Svc-->>API: 기존 jobId 반환
    else 신규
        Svc->>DB: ExportJob(PENDING) 저장
        Svc->>Redis: XADD export-stream
        Svc-->>API: 새 jobId 반환
    end
    API-->>Owner: jobId

    Owner->>API: GET /owner/sales/export/{jobId} (폴링)

    Redis-->>Worker: XREADGROUP
    Worker->>DB: 주문 데이터 조회
    Worker->>S3: 엑셀 파일 업로드
    Worker->>DB: status=COMPLETED, file_key 저장

    Owner->>API: GET /owner/sales/export/{jobId}
    API->>S3: presignGetObject(file_key)
    API-->>Owner: Presigned Download URL
```

---

## 4. 역할 기반 접근 제어 (AS-IS)

```mermaid
flowchart TB
    subgraph Public["공개 (인증 불필요)"]
        Login["/api/v1/auth/login"]
        Signup["/api/v1/auth/signup"]
        Refresh["/api/v1/auth/token/refresh"]
        Health["/actuator/health"]
        Swagger["/swagger-ui/**"]
    end

    subgraph Consumer["CONSUMER 전용"]
        Home["/api/v1/home/**"]
        Orders["/api/v1/orders"]
        Favorites["/api/v1/favorites"]
        StoreNsp["/api/v1/stores/{id}/no-show-posts"]
    end

    subgraph Owner["OWNER 전용"]
        Stores["/api/v1/stores (CRUD)"]
        Menus["/api/v1/owner/menus/**"]
        OwnerNsp["/api/v1/owner/no-show-posts/**"]
        OwnerOrders["/api/v1/owner/orders/**"]
        Sales["/api/v1/owner/sales/**"]
    end

    subgraph Authenticated["인증된 사용자 (역할 무관)"]
        Push["/api/v1/push/tokens"]
        Logout["/api/v1/auth/logout"]
    end

    subgraph Admin["ADMIN (정의됨, 미사용 ⚠️)"]
        Ghost["SecurityConfig에\n참조 없음"]
    end

    style Admin fill:#fff3cd,stroke:#ffc107,color:#856404
    style Ghost fill:#fff3cd,stroke:#ffc107,color:#856404
```

---

## 5. 보안 분석 요약 (AS-IS)

```mermaid
mindmap
  root((보안 현황<br/>Score: 6/10))
    강점
      JWE A256GCM 암호화
      Stateless 세션
      Redis 토큰 블랙리스트
      CSRF 비활성화(적절)
      비관적 잠금(주문 재고)
    CRITICAL ⛔
      CORS 와일드카드(*)
        모든 Origin 허용
        모든 Method 허용
        모든 Header 허용
      Rate Limiting 부재
        Brute-force 취약
        DDoS 노출
    HIGH ⚠️
      키 로테이션 미구현
        단일 비밀키
        갱신 불가
      Method-level 인가 부재
        @PreAuthorize 미사용
        리소스 소유권 미검증
      토큰 폐기 비원자성
        Access/Refresh 별도 블랙리스트
        경쟁 조건 가능
    MEDIUM 🔶
      Refresh Token Body 전달
        CSRF 위험 요소
      보안 헤더 미설정
        X-Frame-Options
        Content-Security-Policy
      Redis TTL 레이스
        토큰 만료 vs 블랙리스트 TTL
      ADMIN 역할 유령
        정의만 존재
```

---

## 6. 목표 아키텍처 (TO-BE) — 전체 시스템

```mermaid
flowchart TB
    subgraph Clients["🖥️ 클라이언트"]
        OwnerApp["사장님 앱/웹"]
        ConsumerApp["소비자 앱/웹"]
        AdminApp["관리자 대시보드 🆕"]
    end

    subgraph Edge["엣지 계층 🆕"]
        CDN["CloudFront / CDN\n(정적 자산 캐시)"]
        RateLimit["Rate Limiter\n(Bucket4j / Resilience4j)"]
        CORS["CORS 화이트리스트\n(도메인 명시)"]
    end

    subgraph SpringBoot["Spring Boot 애플리케이션"]
        subgraph SecurityEnhanced["보안 계층 (강화)"]
            JweAuthFilter2["JweAuthFilter"]
            SecurityConfig2["SecurityConfig\n+ CORS 화이트리스트\n+ Security Headers"]
            JweTokenService2["JweTokenService\n+ 키 로테이션 🆕"]
            TokenBlacklist2["TokenBlacklistService\n+ 원자적 폐기 🆕"]
            MethodSec["@PreAuthorize\n리소스 소유권 검증 🆕"]
        end

        subgraph Controllers2["컨트롤러 계층"]
            AllCtrls["기존 컨트롤러 전체\n+ AdminController 🆕\n+ NotificationHistoryController 🆕"]
        end

        subgraph Services2["서비스 계층"]
            AllSvcs["기존 서비스 전체\n+ AdminService 🆕\n+ NotificationHistoryService 🆕\n+ PostExpiryScheduler 🆕"]
        end

        subgraph AsyncWorkers2["비동기 워커 (강화)"]
            NotiWorker3["NotificationDispatchWorker\n+ DB 상태 업데이트 완성 🆕\n+ DLQ 처리 🆕"]
            ExportWorker3["ExportJobDispatchWorker"]
            ExpiryScheduler["PostExpiryScheduler 🆕\n(만료 글 자동 처리)"]
        end

        subgraph Repositories2["리포지토리 계층"]
            AllRepos["기존 리포지토리 전체"]
        end

        subgraph Observability["관측 가능성 🆕"]
            Metrics["Micrometer / Prometheus"]
            Logging["Structured Logging\n(JSON)"]
            Tracing["Distributed Tracing\n(Zipkin/Jaeger)"]
        end
    end

    subgraph Infra2["인프라 (강화)"]
        DB2[("PostgreSQL\n+ PostGIS\n+ Connection Pool\n  최적화")]
        Redis2[("Redis Sentinel/Cluster\n+ 블랙리스트\n+ Streams\n+ 캐시 🆕")]
        S3_2[("S3 / MinIO\n+ Lifecycle 정책 🆕\n+ 고아 객체 정리 🆕")]
        FCM2[("Firebase CM")]
        Monitoring["Grafana + Prometheus 🆕"]
    end

    Clients -->|"HTTPS"| Edge
    Edge -->|"필터링된 요청"| SecurityEnhanced
    SecurityEnhanced --> Controllers2
    Controllers2 --> MethodSec
    MethodSec --> Services2
    Services2 --> Repositories2
    Repositories2 --> DB2

    TokenBlacklist2 --> Redis2
    Services2 --> Redis2
    Redis2 --> AsyncWorkers2
    AsyncWorkers2 --> FCM2
    AsyncWorkers2 --> S3_2
    Services2 --> S3_2

    SpringBoot --> Observability
    Observability --> Monitoring

    style Edge fill:#d4edda,stroke:#28a745,color:#155724
    style AdminApp fill:#d4edda,stroke:#28a745,color:#155724
    style Observability fill:#d4edda,stroke:#28a745,color:#155724
    style Monitoring fill:#d4edda,stroke:#28a745,color:#155724
    style ExpiryScheduler fill:#d4edda,stroke:#28a745,color:#155724
```

---

## 7. 개선 항목 상세 (TO-BE 로드맵)

### 7-1. 보안 강화 — CORS & Rate Limiting

```mermaid
flowchart LR
    subgraph AS_IS["AS-IS ⛔"]
        CorsNow["CORS: *(와일드카드)\n모든 Origin/Method/Header 허용"]
        RateNow["Rate Limit: 없음"]
    end

    subgraph TO_BE["TO-BE ✅"]
        CorsFix["CORS 화이트리스트\nallowedOrigins:\n  - https://app.dandionna.com\n  - https://owner.dandionna.com\nallowedMethods: GET,POST,PATCH,DELETE\nallowedHeaders: Authorization,Content-Type"]
        RateFix["Rate Limiting\nBucket4j + Redis 기반\n로그인: 5회/분\nAPI 일반: 100회/분\n엑셀 Export: 3회/시간"]
    end

    AS_IS -->|"개선"| TO_BE
    style AS_IS fill:#f8d7da,stroke:#dc3545,color:#721c24
    style TO_BE fill:#d4edda,stroke:#28a745,color:#155724
```

### 7-2. 보안 강화 — 키 로테이션 & 토큰 관리

```mermaid
flowchart TD
    subgraph AS_IS2["AS-IS"]
        SingleKey["단일 SecretKey\n(환경변수 BASE64)"]
        SeparateRevoke["Access/Refresh\n개별 블랙리스트 등록"]
        NoRefreshCookie["Refresh Token\n= Body 전달"]
    end

    subgraph TO_BE2["TO-BE"]
        KeyRotation["키 로테이션\n- kid(Key ID) 헤더 추가\n- 복수 키 동시 유효\n- 정기 교체 스케줄"]
        AtomicRevoke["원자적 토큰 폐기\n- Lua Script 기반\n- Access+Refresh 동시 블랙리스트"]
        HttpOnlyCookie["Refresh Token\n= HttpOnly Secure Cookie\n(SameSite=Strict)"]
    end

    SingleKey -->|"개선"| KeyRotation
    SeparateRevoke -->|"개선"| AtomicRevoke
    NoRefreshCookie -->|"개선"| HttpOnlyCookie

    style AS_IS2 fill:#fff3cd,stroke:#ffc107,color:#856404
    style TO_BE2 fill:#d4edda,stroke:#28a745,color:#155724
```

### 7-3. 권한 강화 — Method-level Authorization

```mermaid
flowchart LR
    subgraph AS_IS3["AS-IS"]
        UrlOnly["URL 패턴 기반만\n/owner/** → OWNER\n/home/** → CONSUMER\n리소스 소유권 미검증"]
    end

    subgraph TO_BE3["TO-BE"]
        MethodAuth["Method-level 인가\n@PreAuthorize 적용"]
        ResourceOwnership["리소스 소유권 검증\n매장 주인 확인\n주문 소유자 확인"]
        AdminRole["ADMIN 역할 활성화\n관리자 API 추가\n사용자/매장 관리"]
    end

    AS_IS3 --> MethodAuth
    AS_IS3 --> ResourceOwnership
    AS_IS3 --> AdminRole

    style AS_IS3 fill:#fff3cd,stroke:#ffc107,color:#856404
    style TO_BE3 fill:#d4edda,stroke:#28a745,color:#155724
```

### 7-4. 인프라 & 운영 강화

```mermaid
flowchart TD
    subgraph Infra_ASIS["AS-IS"]
        NoScheduler["만료 처리 없음\n(expire_at 지나도 OPEN 유지)"]
        NoOrphanClean["S3 고아 객체 방치"]
        NoObserve["모니터링 미구현"]
        NoDLQ["알림 재시도 실패 시\n별도 처리 없음"]
        NoSecHeaders["보안 헤더 미설정"]
    end

    subgraph Infra_TOBE["TO-BE"]
        Scheduler["PostExpiryScheduler\n@Scheduled(fixedRate)\nexpire_at < now → EXPIRED"]
        OrphanClean["S3 Lifecycle 정책\n+ 주기적 고아 객체 정리 배치"]
        Observe["관측 가능성 스택\nMicrometer → Prometheus → Grafana\n+ 구조화 로깅(JSON)\n+ 분산 추적(Zipkin)"]
        DLQ["Dead Letter Queue\n재시도 소진 → DLQ 이동\n+ 관리자 알림"]
        SecHeaders["Security Headers\nX-Content-Type-Options: nosniff\nX-Frame-Options: DENY\nStrict-Transport-Security\nContent-Security-Policy"]
    end

    NoScheduler -->|"구현"| Scheduler
    NoOrphanClean -->|"구현"| OrphanClean
    NoObserve -->|"구현"| Observe
    NoDLQ -->|"구현"| DLQ
    NoSecHeaders -->|"구현"| SecHeaders

    style Infra_ASIS fill:#f8d7da,stroke:#dc3545,color:#721c24
    style Infra_TOBE fill:#d4edda,stroke:#28a745,color:#155724
```

### 7-5. DTO & 검증 통일

```mermaid
flowchart LR
    subgraph ASIS_DTO["AS-IS"]
        Scattered["검증 메시지 분산\n각 DTO별 다른 형식"]
        NoPageStandard["페이지네이션 응답\n통일되지 않음"]
    end

    subgraph TOBE_DTO["TO-BE"]
        Unified["공통 Validation 메시지\nValidationMessages.java\n또는 messages.properties"]
        PageWrapper["통일된 페이지 응답\nPageResponse<T>\n{ content, page, size,\n  totalElements, totalPages }"]
    end

    ASIS_DTO --> TOBE_DTO
    style ASIS_DTO fill:#fff3cd,stroke:#ffc107,color:#856404
    style TOBE_DTO fill:#d4edda,stroke:#28a745,color:#155724
```

---

## 8. 데이터베이스 ERD (AS-IS)

```mermaid
erDiagram
    users {
        UUID id PK
        VARCHAR login_id UK
        VARCHAR password_hash
        user_role role
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    owner_profiles {
        BIGINT id PK
        UUID user_id FK
        VARCHAR name
        VARCHAR phone
    }

    consumer_profiles {
        BIGINT id PK
        UUID user_id FK
        VARCHAR name
        VARCHAR phone
    }

    stores {
        UUID id PK
        UUID owner_user_id FK
        VARCHAR store_name
        VARCHAR address
        DECIMAL lat
        DECIMAL lon
        GEOMETRY geom "GENERATED"
        VARCHAR phone
        TIME open_time
        TIME close_time
        VARCHAR image_key
        image_status image_status
    }

    menus {
        BIGINT id PK
        UUID store_id FK
        VARCHAR name
        INT price
        TEXT description
        VARCHAR image_key
    }

    no_show_posts {
        BIGINT id PK
        UUID store_id FK
        BIGINT menu_id FK
        INT qty_total
        INT qty_remaining
        INT discount_rate
        INT original_unit_price
        INT discounted_unit_price
        no_show_post_status status
        TIMESTAMP expire_at
    }

    no_show_post_history {
        BIGINT id PK
        BIGINT post_id FK
        TEXT snapshot "변경 전 상태"
    }

    no_show_orders {
        UUID id PK
        VARCHAR order_no
        UUID consumer_user_id FK
        UUID store_id FK
        VARCHAR menu_names
        INT total_price
        INT paid_amount
        VARCHAR payment_method
        payment_status payment_status
        no_show_order_status status
        TIMESTAMP visit_time
        TIMESTAMP paid_at
    }

    no_show_order_items {
        BIGINT id PK
        UUID order_id FK
        BIGINT no_show_post_id FK
        BIGINT menu_id FK
        VARCHAR menu_name
        INT quantity
        INT unit_price
        INT discount_percent
    }

    favorites {
        UUID store_id PK
        UUID consumer_user_id PK
    }

    push_tokens {
        BIGINT id PK
        UUID user_id FK
        platform platform
        VARCHAR device_id
        VARCHAR fcm_token
        VARCHAR user_agent
        TIMESTAMP last_seen_at
    }

    notifications {
        BIGINT id PK
        UUID sender_user_id FK
        VARCHAR category
        VARCHAR title
        TEXT body
        INT priority
    }

    notification_targets {
        BIGINT id PK
        BIGINT notification_id FK
        UUID target_user_id FK
        VARCHAR status "QUEUED/SENT/FAILED"
        INT retry_count
        TEXT error_log
    }

    export_jobs {
        BIGINT id PK
        UUID store_id FK
        UUID requested_by FK
        VARCHAR request_hash
        export_status status
        VARCHAR file_key
        TIMESTAMP expires_at
    }

    users ||--|| owner_profiles : "has"
    users ||--|| consumer_profiles : "has"
    users ||--o{ push_tokens : "owns"
    users ||--o{ favorites : "favorites"
    users ||--o{ notifications : "creates"
    users ||--o{ export_jobs : "requests"

    notifications ||--o{ notification_targets : "targets"
    users ||--o{ notification_targets : "receives"

    stores ||--|| owner_profiles : "owned_by"
    stores ||--o{ menus : "has"
    stores ||--o{ no_show_posts : "has"
    stores ||--o{ export_jobs : "exports"

    no_show_posts ||--o{ no_show_order_items : "sold_with"
    no_show_posts ||--o{ no_show_post_history : "history"
    no_show_orders ||--o{ no_show_order_items : "includes"
    no_show_orders }o--|| users : "consumer"
    no_show_orders }o--|| stores : "at_store"
    favorites }o--|| stores : "watches"
    menus ||--o{ no_show_posts : "referenced"
```

---

## 9. 개선 우선순위 매트릭스

```mermaid
quadrantChart
    title 개선 항목 우선순위 (영향도 vs 구현 난이도)
    x-axis "낮은 난이도" --> "높은 난이도"
    y-axis "낮은 영향도" --> "높은 영향도"
    quadrant-1 "우선 추진"
    quadrant-2 "전략적 계획"
    quadrant-3 "점진적 개선"
    quadrant-4 "후순위"
    "CORS 화이트리스트": [0.2, 0.95]
    "Security Headers": [0.15, 0.7]
    "Rate Limiting": [0.4, 0.9]
    "만료 스케줄러": [0.3, 0.6]
    "@PreAuthorize": [0.45, 0.75]
    "키 로테이션": [0.7, 0.8]
    "HttpOnly Cookie": [0.5, 0.65]
    "원자적 토큰폐기": [0.55, 0.7]
    "관측 가능성": [0.75, 0.85]
    "S3 고아 정리": [0.35, 0.35]
    "DLQ 처리": [0.6, 0.5]
    "DTO 통일": [0.45, 0.3]
    "ADMIN API": [0.65, 0.55]
    "알림 이력 API": [0.5, 0.4]
```

---

## 10. 구현 단계별 로드맵

```mermaid
gantt
    title 개선 로드맵
    dateFormat YYYY-MM-DD
    axisFormat %m/%d

    section Phase 1 - 긴급 보안
    CORS 화이트리스트 설정          :crit, cors, 2026-02-24, 1d
    Security Headers 추가           :crit, headers, 2026-02-24, 1d
    Rate Limiting (Bucket4j)        :crit, rate, 2026-02-25, 3d

    section Phase 2 - 인가 강화
    @PreAuthorize 도입              :auth1, 2026-02-28, 3d
    리소스 소유권 검증              :auth2, after auth1, 2d
    ADMIN 역할 활성화               :auth3, after auth2, 2d

    section Phase 3 - 토큰 보안
    키 로테이션 구현                :key, 2026-03-05, 4d
    원자적 토큰 폐기 (Lua)         :atomic, after key, 2d
    Refresh → HttpOnly Cookie       :cookie, after atomic, 2d

    section Phase 4 - 운영 안정화
    만료 글 자동 스케줄러           :expire, 2026-03-12, 2d
    S3 고아 객체 정리               :s3clean, after expire, 2d
    알림 DLQ & 재시도 완성          :dlq, 2026-03-12, 3d
    알림 이력 조회 API              :notiapi, after dlq, 2d

    section Phase 5 - 관측 & 품질
    Micrometer + Prometheus 연동    :metric, 2026-03-19, 3d
    구조화 로깅 (JSON)              :log, 2026-03-19, 2d
    DTO/Validation 통일             :dto, 2026-03-22, 3d
    통합 테스트 보강                :test, 2026-03-25, 4d
```

---

## 부록: 기술 스택 요약

| 구분 | AS-IS | TO-BE (제안) |
|------|-------|-------------|
| **Language** | Java 17 | Java 17+ |
| **Framework** | Spring Boot 3.x | Spring Boot 3.x |
| **Auth** | JWE (A256GCM) | JWE + 키 로테이션 + HttpOnly Cookie |
| **DB** | PostgreSQL + PostGIS | + Connection Pool 최적화 |
| **Cache/Queue** | Redis (Blacklist + Streams) | Redis Sentinel/Cluster + 캐시 계층 |
| **Storage** | S3/MinIO | + Lifecycle 정책 + 정리 배치 |
| **Push** | Firebase CM (직접 호출 + 워커) | + DLQ + 재시도 완성 |
| **보안** | URL 패턴 인가, CORS *, 헤더 없음 | 화이트리스트, @PreAuthorize, 보안 헤더 |
| **모니터링** | 없음 | Prometheus + Grafana + 구조화 로깅 |
| **Rate Limit** | 없음 | Bucket4j + Redis |
| **CI/CD** | GitHub Actions (테스트만) | + 빌드/배포 파이프라인 |
