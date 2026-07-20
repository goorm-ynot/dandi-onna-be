# 운영 가이드

현재 운영 기준은 Ubuntu 단일 서버에서 `bootJar + systemd` 로 실행하는 방식이다. 이 저장소에는 systemd 유닛 템플릿과 설치 스크립트가 포함되어 있다. 현재 작업 환경에서 `systemctl status dandionna.service` 를 확인한 결과, OS에는 아직 실제 서비스가 등록되어 있지 않았다. 즉, 템플릿은 준비되어 있지만 서버 적용은 별도 설치가 필요하다.

## 주요 파일

- `deploy/systemd/dandionna.service`: 메인 서비스 유닛
- `deploy/systemd/install.sh`: systemd 유닛 설치 + enable 스크립트
- `deploy/systemd/dandionna-restart-if-running.service`: 실행 중일 때만 재시작하는 oneshot 유닛
- `deploy/systemd/dandionna-restart.timer`: 매일 11시 재시작 타이머
- `deploy/bin/deploy.sh`: 실행 중일 때만 재시작하는 배포 스크립트
- `deploy/bin/rollback.sh`: 실행 중일 때만 재시작하는 롤백 스크립트
- `deploy/env/dandionna.env.example`: 운영 환경 변수 예시
- `deploy/nginx/dandionna.conf`: Nginx reverse proxy 예시

## 운영 정책

- 비정상 종료/예상치 못한 종료 자동 복구
  - `Restart=always`
- 서버 부팅 후 자동 시작
  - `systemctl enable dandionna.service`
  - 단, `/etc/dandionna/dandionna.env` 와 `/opt/dandionna/current/dandionna-app.jar` 가 준비되어 있어야 실제 기동된다.
- 수동 정지는 유지 가능
  - `systemctl stop dandionna.service` 는 명시적인 관리 작업으로 취급되므로, 다시 `start` 하기 전까지 서비스는 올라오지 않는다.
- 11시 재시작은 서비스 active 상태일 때만 수행

## 서버 디렉터리 구조

- `/opt/dandionna/releases/{release-id}`
- `/opt/dandionna/current`
- `/etc/dandionna/dandionna.env`
- `/etc/dandionna/firebase-service-account.json`

## 운영 환경 변수

최소 필수 값:

- `JAVA_BIN=/usr/lib/jvm/java-21-openjdk-amd64/bin/java`
- `SPRING_PROFILES_ACTIVE=prod`
- `SERVER_PORT=8080`
- `DATABASE_URL`
- `DATABASE_ID`
- `DATABASE_PW`
- `REDIS_URL`
- `REDIS_PORT`
- `REDIS_PASSWORD`
- `JWE_SECRET_B64`
- `FIREBASE_KEY_PATH`
- `MINIO_BUCKET`
- `MINIO_REGION`
- `MINIO_ENDPOINT`
- `MINIO_PUBLIC_ENDPOINT`
- `MINIO_ACCESS_KEY`
- `MINIO_SECRET_KEY`
- `APP_CORS_ALLOWED_ORIGINS`

## 권장 설치 순서

1. Java 21 설치
2. Nginx 설치
3. 서버 timezone 을 `Asia/Seoul` 로 설정
4. `deploy/systemd/install.sh` 를 root 로 실행
5. 스크립트가 `dandionna` 사용자/그룹, `/opt/dandionna`, `/etc/dandionna`, `/etc/dandionna/dandionna.env.example` 를 준비했는지 확인
6. `deploy/env/dandionna.env.example` 기준으로 `/etc/dandionna/dandionna.env` 작성
7. Firebase 서비스 계정 파일 배치
8. `./gradlew clean test bootJar` 후 jar 를 `/opt/dandionna/current/dandionna-app.jar` 위치에 배치하거나 `deploy/bin/deploy.sh` 로 릴리스를 stage
9. `systemctl start dandionna.service`
10. `systemctl status dandionna.service`
11. `systemctl status dandionna-restart.timer`
12. `journalctl -u dandionna.service -f` 로 부팅 로그 확인

## install.sh 동작 방식

`deploy/systemd/install.sh` 는 이제 다음 순서로 동작한다.

1. `dandionna` 사용자/그룹이 없으면 생성한다.
2. `/opt/dandionna`, `/opt/dandionna/releases`, `/etc/dandionna` 디렉터리를 만든다.
3. `/etc/dandionna/dandionna.env.example` 를 배치한다.
4. systemd 유닛을 설치하고 `enable` 한다.
5. `/etc/dandionna/dandionna.env`, Java 21 runtime, `/opt/dandionna/current/dandionna-app.jar` 가 모두 준비됐을 때만 서비스를 시작한다.

즉, 예전처럼 준비가 덜 된 상태에서 `enable --now` 로 바로 재시작 루프에 빠지지 않도록 바꾼 것이다.

만약 아래 두 파일이 없으면 서비스는 설치만 되고 시작되지는 않는다.

- `/etc/dandionna/dandionna.env`
- `/opt/dandionna/current/dandionna-app.jar`

그리고 런타임 Java 가 21 미만이면 jar 는 시작 직후 `UnsupportedClassVersionError` 로 종료된다. 이 프로젝트는 `build.gradle` 에서 Java toolchain 21을 사용하므로, 운영 런타임도 Java 21 이상이어야 한다.

여러 Java 버전이 함께 설치된 서버에서는 `/etc/dandionna/dandionna.env` 에 아래처럼 명시하는 편이 가장 안전하다.

```bash
JAVA_BIN=/usr/lib/jvm/java-21-openjdk-amd64/bin/java
```

준비가 끝난 뒤에는 아래 명령으로 시작하면 된다.

```bash
sudo systemctl start dandionna.service
sudo systemctl status dandionna.service
sudo journalctl -u dandionna.service -f
```

## 기본 배포 절차

1. `./gradlew clean test bootJar`
2. 산출물 `build/libs/dandionna-app.jar` 생성 확인
3. jar 를 서버로 전송
4. 서버에서 `deploy/bin/deploy.sh <release-id> <jar-path>` 실행
5. 서비스가 active 상태면 restart + health check 수행
6. 서비스가 inactive 상태면 새 릴리스만 staging 하고 앱은 켜지지 않음

## 롤백 절차

1. 대상 릴리스 확인
2. 서버에서 `deploy/bin/rollback.sh <release-id>` 실행
3. 서비스가 active 상태면 restart + health check 수행
4. 서비스가 inactive 상태면 symlink 만 변경하고 앱은 켜지지 않음

## 자주 쓰는 명령

```bash
systemctl start dandionna.service
systemctl stop dandionna.service
systemctl status dandionna.service
journalctl -u dandionna.service -f
sudo ./deploy/systemd/install.sh
systemctl enable --now dandionna-restart.timer
systemctl disable --now dandionna-restart.timer
```
