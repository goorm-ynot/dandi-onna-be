# 단디온나 백엔드

노쇼로 발생한 음식점 재고 손실을 실시간 판매 기회로 전환하는 Spring Boot 기반 O2O 예약·주문 플랫폼 백엔드입니다.

사장님(OWNER)과 소비자(CONSUMER) 역할을 JWE 기반 토큰으로 인증하고, 매장/메뉴, 노쇼 게시글, 예약 게시, 주문, 알림, 매출 엑셀 export, 운영/성능 측정 흐름을 관리합니다.

## 문서

- [PORTFOLIO.md](PORTFOLIO.md) - 백엔드 구현 범위와 기술적 기여 요약
- [docs/repo-notes.md](docs/repo-notes.md) - 브랜치 기준, 구현 범위, 검토 순서
- [doc/database.md](doc/database.md) - ERD/테이블 설명
- [doc/api-spec.md](doc/api-spec.md) - REST API 명세
- [doc/architecture.md](doc/architecture.md) - 아키텍처와 Mermaid 다이어그램
- [doc/operations.md](doc/operations.md) - Ubuntu 단일 서버 운영/배포 가이드
- [doc/performance-measurement-plan.md](doc/performance-measurement-plan.md) - k6 기반 성능 측정 체계, 데이터셋 전략, 결과 템플릿
- [doc/performance-results-2026-04-15.md](doc/performance-results-2026-04-15.md) - 당시 baseline 수치와 Grafana 관찰 결과 요약
- [doc/exception-policy.md](doc/exception-policy.md) - 예외 정책
- [doc/tech-stack.md](doc/tech-stack.md) - 사용 기술 목록
- [doc/refactoring-roadmap.md](doc/refactoring-roadmap.md) - 향후 리팩터링 아이디어

## 핵심 기능 요약

### Auth / Security

- Spring Security 기반 Stateless 보안 구조
- JWE Access/Refresh Token 발급 및 검증
- Redis token blacklist
- OWNER / CONSUMER / ADMIN 역할 기반 접근 제어
- Redis INCR/EXPIRE 기반 RateLimit

### NoShow Domain

- 노쇼 게시글 즉시 등록
- 프리셋 기반 노쇼 예약 게시
- 소비자 노쇼 주문 생성
- PESSIMISTIC_WRITE 기반 게시글 잠금 조회
- 방문 시간 / 금액 / 할인율 / 원가 서버 재검증
- 잔여 수량 차감 및 sold_out 상태 전이

### Notification / Export

- FCM 알림 발송
- Redis Stream 기반 알림 worker
- 알림 이력 저장 및 DLQ 이동 / 수동 replay 구조
- Redis Stream 기반 매출 엑셀 export worker
- Apache POI 기반 엑셀 생성
- S3-compatible storage 업로드 및 presigned download URL 발급

### Store / Menu / Image

- 사장님 매장 관리
- 단품/세트 메뉴 관리
- effectiveStatus 계산
- 노쇼 프리셋 관리
- Redis upload token과 ETag 검증 기반 메뉴 이미지 임시 업로드 / 확정 / 최종 반영 흐름

### Location / Observability / Performance

- PostGIS 기반 매장 거리 계산
- 소비자 홈 매장 조회
- Prometheus business metric
- MDC 기반 JSON 구조화 로그
- k6 + Docker 기반 성능 측정 스크립트
- small / medium / large seed SQL

## 실행 가이드

```bash
# 환경 변수 예시
export SPRING_PROFILES_ACTIVE=dev
export DATABASE_URL=jdbc:postgresql://localhost:5432/dandi_db
export DATABASE_ID=appuser
export DATABASE_PW=change-me
export REDIS_URL=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=change-me
export SERVER_PORT=8080
export FIREBASE_KEY_PATH=/path/to/firebase-service-account.json
export JWE_SECRET_B64=replace-with-base64-secret
export MINIO_BUCKET=dandi-bucket
export MINIO_REGION=us-east-1
export MINIO_ENDPOINT=http://localhost:19090
export MINIO_PUBLIC_ENDPOINT=http://host.docker.internal:19090
export MINIO_ACCESS_KEY=change-me
export MINIO_SECRET_KEY=change-me

# 빌드 & 실행
./gradlew clean build
./gradlew bootRun
```

운영 배포 산출물은 아래 명령으로 생성합니다.

```bash
./gradlew clean test bootJar
# 결과물: build/libs/dandionna-app.jar
```

## 성능 측정 빠른 시작

성능 측정은 `k6 + Docker` 기준으로 통일되어 있습니다. 자세한 기준과 해석 방법은 [doc/performance-measurement-plan.md](doc/performance-measurement-plan.md)를 기준 문서로 사용합니다.

```bash
# 1) 성능 측정용 env 준비
cd /home/rua/Project/dandi/v1/dandi-onna-be
cp env/perf.local.env.example env/perf.local.env

# 2) env/perf.local.env 값을 현재 머신 기준으로 수정
#    - DATABASE/REDIS/MINIO/JWE/FIREBASE 경로
#    - SERVER_PORT=18080
#    - MINIO_PUBLIC_ENDPOINT=http://host.docker.internal:19090

# 3) Infra 실행
cd /home/rua/Project/dandi/Infra
./infra.sh start

# 4) 앱 실행
cd /home/rua/Project/dandi/v1/dandi-onna-be
./scripts/perf/run-app.sh

# 5) 상태 확인
./scripts/perf/check-stack.sh

# 6) 벤치마크용 데이터셋 적용
./scripts/perf/apply-seed.sh small

# 7) 필수 smoke 실행
./scripts/perf/run-required.sh smoke

# 8) 필수 measure 실행
./scripts/perf/run-required.sh measure
```

결과 파일은 `artifacts/perf/<timestamp>/<scenario>/<profile>/summary.{json,md}` 아래에 저장됩니다.

주요 스크립트는 다음과 같습니다.

- `./scripts/perf/run-app.sh`: `env/perf.local.env`를 읽고 `perf` 프로필로 앱 실행
- `./scripts/perf/check-stack.sh`: Docker 컨테이너와 앱 health 확인
- `./scripts/perf/apply-seed.sh <reset|small|medium|large>`: 성능용 seed 적용
- `./scripts/perf/run-required.sh [smoke|measure]`: 필수 5개 시나리오 묶음 실행
- `./scripts/perf/run-k6.sh <scenario> <profile>`: 개별 시나리오 실행

## 운영 배포 자산

- `deploy/systemd/` - `dandionna.service`, 설치 스크립트, 매일 11시 재시작 timer 템플릿
- `deploy/bin/` - 실행 중일 때만 restart 하는 배포/롤백 스크립트
- `deploy/env/dandionna.env.example` - 운영 환경 변수 예시
- `deploy/nginx/dandionna.conf` - Nginx reverse proxy 예시

## 구현 범위 기준

- Storage는 AWS SDK S3 API 기반이며, MinIO 등 S3-compatible endpoint에 연결할 수 있도록 구성했습니다.
- 주문 생성은 dummy payment 기반으로 처리합니다.
- 위치 조회는 `ST_DistanceSphere` 기반 거리 계산과 거리순 정렬 기준입니다.
- 알림 retry는 attempt 증가, requeue, DLQ 이동, 수동 replay 구조를 포함합니다.
- k6 성능 측정 체계와 seed SQL은 구성되어 있으며, 성능 수치는 별도 결과 파일 기준으로 기록합니다.

## 테스트

기준 브랜치에서 다음 테스트 결과를 확인했습니다.

```text
./gradlew test --no-daemon
59 tests, 0 failures, 0 errors, 2 skipped
```

인프라 의존 테스트는 `DANDI_RUN_INFRA_TESTS=true` 환경변수가 있을 때만 실행됩니다.
