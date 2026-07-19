## 변경 이유

- 해결하려는 문제와 변경 이유를 적어 주세요.

## 주요 변경

- 변경한 내용을 검토 가능한 단위로 정리해 주세요.

## 연결 이슈

- Closes #

## 검증 결과

- [ ] Java 21 사용
- [ ] `./gradlew clean test bootJar --no-daemon`
- [ ] main/test source compile
- [ ] 단위 테스트
- [ ] bootJar 생성
- [ ] 필요한 수동 검증

```text
실행 명령과 결과
```

## DB·데이터 영향

- [ ] 영향 없음
- [ ] Flyway migration과 entity 정합성 확인
- [ ] 운영 데이터 영향과 롤백 계획 작성

설명:

## 보안·외부 연동 영향

- [ ] 인증·인가·CORS·rate limit 영향 검토
- [ ] 비밀정보가 코드·로그·문서에 포함되지 않음
- [ ] PostgreSQL/PostGIS·Redis·S3/MinIO·Firebase 영향 검토
- [ ] 장애 시 동작과 테스트 대체 수단 확인

설명:

## 배포·운영 영향

- [ ] 영향 없음
- [ ] 환경변수·systemd·Nginx·Docker 영향 기록
- [ ] 모니터링 및 성능 영향 검토

설명:

## 문서

- [ ] 문서 변경 불필요
- [ ] 관련 `doc/` 문서 갱신
- [ ] `doc/project-log.md` 갱신
- [ ] 로컬 문서 링크 확인

## 실행하지 못한 검증

- 필요한 환경과 실행하지 못한 이유를 적어 주세요.

## 롤백

- 안전하게 되돌리는 방법을 적어 주세요.

## 최종 확인

- [ ] 작성자 외 reviewer 승인 또는 단독 유지보수 예외 사유 기록
- [ ] 하나의 주요 목적만 포함함
- [ ] feature 브랜치에 develop merge를 반복하지 않음
- [ ] 임시 산출물과 비밀정보가 없음
- [ ] diff를 직접 재검토함
