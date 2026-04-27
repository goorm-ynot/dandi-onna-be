# 단디온나 백엔드

노쇼 방지 및 실시간 할인 판매를 지원하는 Spring Boot 백엔드입니다. 사장님(OWNER)·소비자(CONSUMER) 역할을 JWT로 인증하고, 매장/노쇼 글/주문/알림 전 과정을 관리합니다.

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

성능 측정은 `k6 + Docker` 기준으로 통일되어 있습니다. 자세한 기준과 해석 방법은 [doc/performance-measurement-plan.md](doc/performance-measurement-plan.md) 를 기준 문서로 사용합니다.

문서 안의 `4-3. 검증 아키텍처 한눈에 보기`, `4-4. 한 번의 검증 사이클` 섹션에는 Mermaid 다이어그램이 포함되어 있어, 어떤 프로세스가 어디서 실행되고 어떤 순서로 검증되는지 시각적으로 확인할 수 있습니다.

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

- `./scripts/perf/run-app.sh`: `env/perf.local.env` 를 읽고 `perf` 프로필로 앱 실행
- `./scripts/perf/check-stack.sh`: Docker 컨테이너와 앱 health 확인
- `./scripts/perf/apply-seed.sh <reset|small|medium|large>`: 성능용 seed 적용
- `./scripts/perf/run-required.sh [smoke|measure]`: 필수 5개 시나리오 묶음 실행
- `./scripts/perf/run-k6.sh <scenario> <profile>`: 개별 시나리오 실행

## 문서 모음

- [doc/database.md](doc/database.md) - 최종 ERD/테이블 설명
- [doc/api-spec.md](doc/api-spec.md) - REST API 명세
- [doc/architecture.md](doc/architecture.md) - 아키텍처 & Mermaid 다이어그램
- [doc/operations.md](doc/operations.md) - Ubuntu 단일 서버 운영/배포 가이드
- [doc/performance-measurement-plan.md](doc/performance-measurement-plan.md) - k6 기반 성능 측정 체계, 데이터셋 전략, 결과 템플릿
- [doc/exception-policy.md](doc/exception-policy.md) - 예외 정책
- [doc/tech-stack.md](doc/tech-stack.md) - 사용 기술 목록
- [doc/refactoring-roadmap.md](doc/refactoring-roadmap.md) - 향후 리팩터링 아이디어

## 핵심 기능 요약

- 사장님: 매장/메뉴 CRUD, 노쇼 글 배치 등록, 주문 조회·완료 처리, 주문 알림 수신
- 소비자: 매장 즐겨찾기, 위치 기반 주문 가능 매장 조회, 노쇼 주문·결제, 즐겨찾기 알림 수신
- 이미지: S3 Presigned URL 발급/확정
- 알림: FCM으로 노쇼 주문(사장님) 알림, 즐겨찾기 소비자 노쇼 글 알림

## 운영 배포 자산

- `deploy/systemd/` - `dandionna.service`, 설치 스크립트, 매일 11시 재시작 timer 템플릿
- `deploy/bin/` - 실행 중일 때만 restart 하는 배포/롤백 스크립트
- `deploy/env/dandionna.env.example` - 운영 환경 변수 예시
- `deploy/nginx/dandionna.conf` - Nginx reverse proxy 예시
