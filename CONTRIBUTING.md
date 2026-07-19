# 단디온나 공식 작업 규칙

이 문서는 단디온나 백엔드 저장소의 이슈, 브랜치, 커밋, Pull Request, 검증, 병합 및 문서 관리 규칙을 정의합니다.

## 1. 기본 원칙

- 모든 변경은 추적 가능한 이슈에서 시작합니다.
- `main`, `develop`에 직접 commit 또는 push하지 않습니다.
- 한 브랜치와 PR은 하나의 주요 목적만 다룹니다.
- 기능 변경에는 테스트와 관련 문서를 함께 갱신합니다.
- 비밀정보와 실제 운영 데이터를 저장소에 넣지 않습니다.
- 운영 배포와 운영 데이터 변경에는 별도 사람 승인이 필요합니다.

## 2. 브랜치 모델

- `main`: 배포 가능한 안정 버전입니다. 원칙적으로 `develop`에서 승격하는 PR만 받습니다.
- `develop`: 다음 배포를 위한 통합 브랜치입니다.
- 작업 브랜치: 최신 `origin/develop`에서 생성합니다.

작업 브랜치 이름은 소문자 kebab-case를 사용합니다.

```text
feat/<issue>-<summary>
fix/<issue>-<summary>
refactor/<issue>-<summary>
migration/<issue>-<summary>
docs/<issue>-<summary>
chore/<issue>-<summary>
recovery/<issue-or-summary>
codex/<issue-or-summary>
```

개인 작업 브랜치는 최신 develop이 필요할 때 rebase로 동기화합니다. develop을 feature 브랜치에 반복 merge해 중복 계보를 만들지 않습니다. 공유 브랜치는 rebase 전에 참여자와 조율합니다.

## 3. 이슈 규칙

이슈에는 작업 유형과 목적, 배경, 포함·제외 범위, 완료 기준, 테스트 계획, DB·보안·외부 서비스·배포 영향, 관련 문서를 기록합니다.

작업이 끝나면 체크리스트를 실제 결과에 맞게 갱신합니다. PR 본문에 `Closes #<issue>`를 사용해 병합 시 이슈를 닫습니다. 남은 작업은 후속 이슈로 분리합니다.

## 4. 커밋 규칙

권장 형식:

```text
<type>(<scope>): <summary> (#<issue>)
```

허용 type은 `feat`, `fix`, `refactor`, `test`, `docs`, `migration`, `perf`, `build`, `ci`, `chore`입니다.

```text
feat(menu): add set menu domain (#38)
fix(redis): stabilize worker shutdown (#42)
docs: consolidate documentation under doc
```

커밋은 독립적으로 검토할 수 있어야 합니다. 생성 파일, 무관한 formatting, 임시 build 산출물을 기능 commit에 섞지 않습니다.

## 5. Pull Request 규칙

PR에는 다음을 반드시 포함합니다.

- 변경 이유와 주요 변경
- `Closes #<issue>` 형식의 연결 이슈
- 테스트 명령과 결과
- DB migration 및 데이터 영향
- 인증·인가·비밀정보 영향
- PostgreSQL/PostGIS, Redis, S3/MinIO, Firebase 영향
- 배포 영향과 롤백 방법
- 실행하지 못한 검증과 이유

코드, DB, 배포, 성능 측정, 문서가 독립적으로 검토 가능한 규모라면 PR을 분리합니다.

PR은 required check 통과, 작성자 외 최소 1명 승인, review thread 해결, 최종 diff 확인 후 squash 또는 rebase로 병합합니다. 승인되지 않은 force push를 하지 않습니다. 병합 후 이슈와 불필요한 원격 작업 브랜치를 정리합니다.

단독 유지보수라 reviewer를 확보할 수 없다면 PR에 예외 이유와 사후 검토 계획을 기록하고, branch ruleset 변경에는 별도 사람 승인을 받습니다.

## 6. 검증 규칙

Java 21과 저장소 Gradle wrapper를 사용합니다.

```bash
./gradlew clean test bootJar --no-daemon
```

기본 검증은 main/test source compile, 단위 테스트, 기본 Spring context 검증, bootJar 생성을 포함합니다.

인프라 테스트는 실제 서비스에 임의로 연결하지 않습니다. 실행하지 못했다면 `로컬 실행 가능`, `Docker/Testcontainers 필요`, `환경변수 필요`, `실제 외부 서비스 필요`, `테스트 없음` 중 하나로 PR에 표시합니다.

## 7. DB 및 데이터 규칙

- schema 변경은 Flyway migration으로 관리합니다.
- 공유된 migration을 수정하지 않고 새 버전을 추가합니다.
- entity와 migration을 함께 검토합니다.
- 운영 migration에 테스트 계정과 seed를 넣지 않습니다.
- destructive SQL에는 영향 범위, 백업 및 롤백 계획이 필요합니다.
- 성능 seed와 reset은 격리 DB에서만 실행합니다.
- 운영 DB 쓰기는 별도 승인 없이는 수행하지 않습니다.

## 8. 보안 및 외부 연동 규칙

- `.env`, 토큰, 인증서, private key, Firebase key, 클라우드 자격증명을 commit하지 않습니다.
- 예시 파일에는 실제 값을 넣지 않습니다.
- 로그와 성능 결과에 토큰, 비밀번호, 요청·응답 본문 또는 사용자 식별정보를 남기지 않습니다.
- 인증·인가·CORS·rate limit 변경은 실패 정책과 우회 가능성을 PR에 설명합니다.
- 외부 서비스 장애 동작을 명시하고 가능하면 adapter/fake로 테스트합니다.

## 9. 문서 규칙

- 문서 표준 디렉터리는 `doc/`이며 새 `docs/` 디렉터리를 만들지 않습니다.
- API 변경은 `doc/api-spec.md`를 갱신합니다.
- schema 변경은 `doc/database.md`를 갱신합니다.
- 운영 변경은 `doc/operations.md`를 갱신합니다.
- 중요한 결정은 `doc/project-log.md`에 남깁니다.
- 문서 링크가 실제 파일을 가리키는지 확인합니다.

## 10. 완료 정의

- 이슈 완료 기준 충족
- 관련 코드·테스트·문서 갱신
- Java 21 기본 검증 통과
- DB·보안·외부 연동·배포 위험 검토
- 미실행 검증과 롤백 방법 명시
- required check 통과 및 review thread 해결
- 병합 후 이슈 상태와 브랜치 정리

## 11. 예외와 긴급 작업

긴급 수정도 가능한 한 이슈와 PR을 거칩니다. 절차를 생략했다면 이유, 승인자, 검증 결과와 사후 조치를 기록합니다. 운영 배포, force push, production data 변경은 명시적인 사람 승인 없이 수행하지 않습니다.
