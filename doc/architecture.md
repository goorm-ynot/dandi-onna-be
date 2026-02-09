# 아키텍처 (Mermaid)

```mermaid
flowchart LR
  subgraph Client
    OwnerApp[사장님 앱/웹]
    ConsumerApp[소비자 앱/웹]
  end

  subgraph Backend[Spring Boot]
    API[REST Controllers]
    Service[Services]
    Repo[JPA Repositories]
    Upload[UploadService]
    NotifyEnq[NotificationEnqueueService]
    Worker[NotificationDispatchWorker]
    ExportEnq[ExportEnqueueService]
    ExportWorker[ExportDispatchWorker]
  end

  subgraph Infra
    DB[(PostgreSQL + PostGIS)]
    Redis[(Redis
Blacklist + Streams)]
    S3[(S3/MinIO
Presign)]
    FCMApi[(Firebase Cloud Messaging)]
  end

  Client -->|JWT| API
  API --> Service
  Service --> Repo --> DB
  Service --> Upload --> S3

  Service --> NotifyEnq --> DB
  NotifyEnq --> Redis
  Redis --> Worker --> FCMApi
  FCMApi --> Client

  Service --> ExportEnq --> DB
  ExportEnq --> Redis
  Redis --> ExportWorker --> S3

  API --> Redis
```

## 흐름 요약
- JWT 인증 후 사장/소비자 API 호출
- 노쇼 주문/즐겨찾기 알림 → **Redis Stream** 큐 → 워커가 FCM 전송
- 이미지 업로드: Presign → 클라이언트 업로드 → Confirm/ETag 검증
- Redis는 토큰 블랙리스트 및 알림 큐에 사용
- 매출 엑셀 내보내기: **비동기 작업(ExportJob)** → Redis Stream → 워커가 파일 생성 후 Presign URL 제공
