# Dandi-Onna Repository Notes

> 이 문서는 단디온나 백엔드 저장소의 브랜치 기준, 구현 범위, 검토 순서를 정리합니다.

## 1. 기준 브랜치

| 구분 | 내용 |
|---|---|
| 기준 구현 브랜치 | `feat/38-menu-domain-expansion` |
| 문서 브랜치 | `docs/dandi-onna-portfolio` |
| 기본 브랜치 | `main` |
| 저장소 | `goorm-ynot/dandi-onna-be` |

`docs/dandi-onna-portfolio` 브랜치는 `feat/38-menu-domain-expansion`의 구현 상태를 기준으로 문서화했습니다.

## 2. 프로젝트 기간 기준

단디온나는 2025년 11월 정식 발표 이후에도 추가 개발이 이어졌습니다.

정식 발표 당시에는 AWS/S3 기반 구조를 중심으로 설명되었고, 이후 개발 과정에서 S3-compatible storage, 로컬 실행 문서, 운영 배포 자산, 성능 측정 스크립트가 보강되었습니다.

## 3. 구현 범위 요약

현재 기준 브랜치에서 확인 가능한 주요 구현 범위는 다음과 같습니다.

- Spring Security 기반 Stateless 보안 구조
- JWE Access/Refresh Token 발급 및 검증
- Redis token blacklist
- OWNER / CONSUMER / ADMIN 역할 기반 접근 제어
- Redis INCR/EXPIRE 기반 RateLimit
- 노쇼 게시글 즉시 등록
- 프리셋 기반 노쇼 예약 게시
- 소비자 노쇼 주문 생성
- PESSIMISTIC_WRITE 기반 게시글 잠금 조회
- 방문 시간 / 금액 / 할인율 / 원가 서버 재검증
- 잔여 수량 차감 및 sold_out 상태 전이
- FCM 알림 발송
- Redis Stream 기반 알림 worker
- 알림 이력 저장 및 DLQ 이동 / 수동 replay 구조
- Redis Stream 기반 매출 엑셀 export worker
- Apache POI 기반 엑셀 생성
- S3-compatible storage 업로드 및 presigned URL 발급
- 매장 / 메뉴 / 세트 메뉴 / 프리셋 관리
- 메뉴 이미지 임시 업로드 / 확정 / 최종 반영 흐름
- PostGIS 기반 거리 계산
- Prometheus business metric
- MDC 기반 JSON 구조화 로그
- k6 + Docker 기반 성능 측정 스크립트와 seed SQL
- systemd / Nginx / deploy / rollback 운영 자산

## 4. 구현 범위 기준

| 영역 | 현재 기준 |
|---|---|
| Storage | AWS SDK S3 API를 사용하며, MinIO 등 S3-compatible endpoint에 연결할 수 있도록 구성했습니다. |
| Payment | 현재 주문 생성은 dummy payment 기반으로 처리합니다. |
| Location | 현재 구현은 `ST_DistanceSphere` 기반 거리 계산과 거리순 정렬입니다. |
| Notification Retry | 실패 attempt 증가, requeue, DLQ 이동, 수동 replay 구조가 있습니다. |
| Performance | k6 실행 체계와 seed SQL을 정리했습니다. 성능 수치는 별도 결과 파일 기준으로 기록합니다. |
| Monitoring | Prometheus business metric과 MDC JSON 로깅을 구성했습니다. |
| Infra | 백엔드 저장소에는 운영/성능 실행 자산이 있고, 일부 로컬 인프라 compose는 별도 Infra 디렉터리 기준으로 연결됩니다. |

## 5. 주요 커밋

기준 브랜치에서 문서화 근거로 사용한 최근 주요 커밋입니다.

```text
9dfe327 설정: CORS 및 인프라 테스트 조건 정리
cd963c2 기능: 메뉴 도메인 확장 및 노쇼 게시 연동 정리
d0a8e8c 개선: 비동기 워커 종료 처리 안정화
2becd24 기능: 날짜 파싱 및 매출 내보내기 기준 정리
924745c 문서: 운영 및 성능 측정 실행 자산 정리
```

## 6. 검토 순서

코드 검토 시 아래 순서로 보면 구현 의도를 빠르게 확인할 수 있습니다.

1. `PORTFOLIO.md`
2. `src/main/java/com/mvp/v1/dandionna/config/Security/SecurityConfig.java`
3. `src/main/java/com/mvp/v1/dandionna/config/Security/JweTokenService.java`
4. `src/main/java/com/mvp/v1/dandionna/auth/service/AuthService.java`
5. `src/main/java/com/mvp/v1/dandionna/noshow_post/service/NoShowPostService.java`
6. `src/main/java/com/mvp/v1/dandionna/noshow_post/service/NoShowPostScheduleService.java`
7. `src/main/java/com/mvp/v1/dandionna/noshow_order/service/NoShowOrderConsumerService.java`
8. `src/main/java/com/mvp/v1/dandionna/notification/worker/NotificationDispatchWorker.java`
9. `src/main/java/com/mvp/v1/dandionna/export_job/worker/ExportJobDispatchWorker.java`
10. `src/main/java/com/mvp/v1/dandionna/menu/service/MenuService.java`
11. `src/main/java/com/mvp/v1/dandionna/s3/service/MenuImageTempUploadService.java`
12. `src/main/java/com/mvp/v1/dandionna/store/repository/StoreRepository.java`
13. `src/main/java/com/mvp/v1/dandionna/config/MetricsConfig.java`
14. `doc/performance-measurement-plan.md`
15. `scripts/perf/`
16. `deploy/`

## 7. 테스트 기준

기준 브랜치에서 다음 테스트 결과를 확인했습니다.

```text
./gradlew test --no-daemon
59 tests, 0 failures, 0 errors, 2 skipped
```

인프라 의존 테스트는 `DANDI_RUN_INFRA_TESTS=true` 환경변수가 있을 때만 실행됩니다.

## 8. 향후 확장 가능 항목

운영 서비스 수준으로 더 확장한다면 아래 항목을 보강할 수 있습니다.

- 실제 PG 결제 연동
- 위치 조회 반경 필터 정책 추가
- notification retry의 `nextRetryAt` 기반 지연 실행
- Grafana dashboard / alert rule 구성
- k6 측정 결과 수치 기록 및 성능 비교 문서화
- 로컬 Infra compose 자산의 저장소 내 템플릿화
