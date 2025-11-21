# 단디온나 백엔드

노쇼 방지 및 실시간 할인 판매를 지원하는 Spring Boot 백엔드입니다. 사장님(OWNER)·소비자(CONSUMER) 역할을 JWT로 인증하고, 매장/노쇼 글/주문/알림 전 과정을 관리합니다.

## 실행 가이드

```bash
# 환경 변수 예시
export SPRING_PROFILES_ACTIVE=local
export DATABASE_URL=jdbc:postgresql://localhost:5432/dandi
export DATABASE_USERNAME=...
export DATABASE_PASSWORD=...
export REDIS_HOST=localhost
export firebase.key-path=/path/to/firebase-service-account.json

# 빌드 & 실행
./gradlew clean build
./gradlew bootRun
```

## 문서 모음

- [doc/database.md](doc/database.md) - 최종 ERD/테이블 설명
- [doc/api-spec.md](doc/api-spec.md) - REST API 명세
- [doc/architecture.md](doc/architecture.md) - 아키텍처 & Mermaid 다이어그램
- [doc/exception-policy.md](doc/exception-policy.md) - 예외 정책
- [doc/tech-stack.md](doc/tech-stack.md) - 사용 기술 목록
- [doc/refactoring-roadmap.md](doc/refactoring-roadmap.md) - 향후 리팩터링 아이디어

## 핵심 기능 요약

- 사장님: 매장/메뉴 CRUD, 노쇼 글 배치 등록, 주문 조회·완료 처리, 주문 알림 수신
- 소비자: 매장 즐겨찾기, 위치 기반 주문 가능 매장 조회, 노쇼 주문·결제, 즐겨찾기 알림 수신
- 이미지: S3 Presigned URL 발급/확정
- 알림: FCM으로 노쇼 주문(사장님) 알림, 즐겨찾기 소비자 노쇼 글 알림
