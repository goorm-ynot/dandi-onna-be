# 성능 측정 결과 보고서 (2026-04-15)

이 문서는 현재 저장소에 추가한 `k6 + Docker` 기반 성능 측정 체계의 **실제 실행 결과**를 GitHub에서 바로 읽을 수 있게 정리한 보고서다. 원본 summary 파일은 로컬 `artifacts/perf/` 아래에 남기고, Git에는 사람이 비교하기 쉬운 핵심 숫자와 해석만 올린다.

## 1. 이번 작업으로 만들어진 결과물

이번 작업의 결과는 단순히 “스크립트 몇 개 추가”가 아니다. 실제로는 아래 네 묶음이 함께 만들어졌다.

1. 성능 측정 프레임워크
- `scripts/perf/` 아래 공통 모듈, 실행 래퍼, 시나리오, SQL seed

2. 측정 기준 문서
- `doc/performance-measurement-plan.md`
- `README.md`
- `doc/operations.md`

3. 측정이 막히던 코드 불일치 수정
- `GET /api/v1/owner/sales`
- `POST /api/v1/owner/sales/export`
- `PostExpiryScheduler`
- 운영 `bootJar + systemd + Java 21` 실행 경로

4. 실제 수치
- 필수 5개 API의 `smoke` 결과
- 필수 5개 API의 `measure` 결과
- Grafana 기반 운영 관찰값

즉, 이 문서는 “무엇을 만들었는가”와 “그래서 어떤 숫자가 나왔는가”를 한 번에 보여주는 최종 결과물이다.

## 2. 측정 환경 요약

| 항목 | 값 |
| --- | --- |
| 앱 성능 측정 포트 | `18080` |
| 운영 서비스 포트 | `8080` |
| 성능 측정 프로필 | `perf` |
| 운영 서비스 프로필 | `prod` |
| 공식 측정 도구 | `grafana/k6` Docker 컨테이너 |
| 데이터셋 | `small` |
| 기준 일자 | `2026-04-15` |
| 결과 저장 위치 | 로컬 `artifacts/perf/<timestamp>/<scenario>/<profile>/summary.{json,md}` |

이번 보고서의 수치는 `small` 데이터셋 기준이다. 따라서 이 숫자는 “현재 구조가 동작하고, 기본 병목을 가늠할 수 있는 1차 기준선”으로 봐야 한다. 이력서용 최종 수치로 쓸 때는 `medium`, `large` 데이터셋 결과를 함께 남기는 편이 더 좋다.

## 3. 필수 5개 API 측정 결과

### 3-1. 공식 baseline 으로 볼 measure 결과

아래 표는 `measure` 프로필 결과를 사람이 비교하기 좋게 다시 정리한 것이다.

| API | profile | dataset | avg(ms) | p95(ms) | p99(ms) | max(ms) | fail | 비고 |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | --- |
| `GET /api/v1/home/stores` | `measure` | `small` | 14.69 | 20.02 | 26.61 | 96.64 | 0.00% | PostGIS 거리 정렬 + 매장 이미지 presign 포함 |
| `POST /api/v1/orders` | `measure` | `small` | 37.70 | 68.41 | 90.32 | 95.80 | 0.00% | 재고 검증 + 주문/주문아이템 저장 + 알림 enqueue 포함 |
| `POST /api/v1/owner/sales/export` + polling | `measure` | `small` | 19.08 | 63.24 | 86.05 | 91.75 | 0.00% | 비동기 흐름은 ACK/DONE 분리 해석 필요 |
| `GET /api/v1/owner/menus` | `measure` | `small` | 63.74 | 82.02 | 98.46 | 149.14 | 0.00% | 세트 구성 계산 + 메뉴 이미지 presign 포함 |
| `GET /api/v1/stores/{storeId}/no-show-posts` | `measure` | `small` | 23.52 | 27.53 | 34.02 | 109.36 | 0.00% | 매장/메뉴 이미지 presign + 즐겨찾기 여부 조회 포함 |

### 3-2. 비동기 흐름에서 따로 봐야 하는 지표

`sales-export` 는 단순 응답시간보다 아래 두 숫자가 더 중요하다.

| metric | avg(ms) | p95(ms) | p99(ms) | max(ms) | 의미 |
| --- | ---: | ---: | ---: | ---: | --- |
| `export_ack_ms` | 9.05 | 9.54 | 9.59 | 9.60 | export 요청을 받고 `jobId` 를 돌려줄 때까지 |
| `export_done_ms` | 685.33 | 1028.90 | 1029.78 | 1030.00 | 실제 Excel 생성 + 업로드 + 상태 `DONE` 까지 |

이 결과는 “요청 접수는 매우 빠르지만, 실제 파일 완성까지는 최대 약 1초 정도 더 걸린다”는 뜻이다. 즉, export는 synchronous API가 아니라 background worker 구간까지 봐야 제대로 해석된다.

### 3-3. smoke 결과가 의미하는 것

`smoke` 는 빠른 공식 수치가 아니라, **시나리오가 실제로 끝까지 동작하는지 검증한 결과**다.

| API | smoke p95(ms) | fail | 의미 |
| --- | ---: | ---: | --- |
| `GET /api/v1/home/stores` | 94.22 | 0.00% | 조회 시나리오 정상 동작 확인 |
| `POST /api/v1/orders` | 89.46 | 0.00% | 주문 생성 시나리오 정상 동작 확인 |
| `sales-export` | 83.21 | 0.00% | 요청 + polling + DONE 흐름 확인 |
| `GET /api/v1/owner/menus` | 109.40 | 0.00% | 메뉴 목록 조회 경로 정상 동작 확인 |
| `GET /api/v1/stores/{storeId}/no-show-posts` | 85.07 | 0.00% | 매장 상세 조회 경로 정상 동작 확인 |

즉, `smoke` 는 “실행 가능성 검증”, `measure` 는 “기준선 수치 확보”로 이해하면 된다.

## 4. 이번 결과에서 바로 읽을 수 있는 해석

### 4-1. 현재 기준 가장 무거운 조회는 `owner-menus`

`GET /api/v1/owner/menus` 의 `p95 82.02ms`, `p99 98.46ms` 는 필수 5개 중 조회 계열에서 가장 무거운 편이다. 문서와 코드 기준으로 보면 이 API는 단순 목록 조회가 아니라 아래 비용이 합쳐진다.

- 세트 메뉴 구성 계산
- 메뉴 이미지 presign URL 생성
- 상태 필터 적용

즉, 사장님 운영 화면 최적화가 필요할 때 가장 먼저 다시 볼 후보가 `owner-menus` 다.

### 4-2. `home-stores` 는 현재 기준 비교적 안정적이다

`GET /api/v1/home/stores` 는 `p95 20.02ms`, `p99 26.61ms` 로 비교적 안정적이다. 현재 `small` 데이터셋 기준에서는 PostGIS 거리 정렬과 매장 이미지 presign 비용이 포함되어도 큰 병목으로 보이지 않는다. 다만 이 평가는 `small` 기준이므로 `medium`, `large` 에서 거리 정렬과 페이지네이션 비용이 다시 튈 가능성은 남아 있다.

### 4-3. 주문 생성은 읽기보다 무겁지만, 실패 없이 안정적이다

`POST /api/v1/orders` 는 읽기 API보다 비싸지만 `fail 0.00%` 로 안정적으로 동작했다. 현재 기준으로는 “쓰기 API라서 느리다” 수준이지, 즉시 병목으로 볼 정도의 숫자는 아니다.

### 4-4. export 는 응답시간보다 완료시간을 봐야 한다

`sales-export` 는 `http_req_duration` 자체보다 `export_ack_ms` 와 `export_done_ms` 차이가 중요하다.

- ACK: 약 `9ms`
- DONE avg: 약 `685ms`
- DONE p95: 약 `1029ms`

즉 사용자는 버튼을 누르면 거의 바로 job 요청이 접수되지만, 실제 파일을 받기까지는 worker/MinIO 구간이 추가로 필요하다.

## 5. Grafana 관찰값 요약

현재 연결된 Grafana 대시보드 스크린샷 기준으로 읽을 수 있는 내용은 아래와 같다.

### 5-1. 현재 대시보드가 보고 있는 대상

- 라벨상 `instance="host.docker.internal:8080"` 이 보인다.
- 즉 현재 대시보드는 기본적으로 `prod:8080` 운영 서비스 쪽을 보고 있다.
- `perf:18080` 를 따로 띄워 측정했다면, 그 인스턴스는 현재 대시보드에서 분리해서 보이지 않을 수 있다.

### 5-2. 현재 읽을 수 있는 상태

- 앱 UP/DOWN: `1`
- JVM Heap, Active Threads, GC Pause: 수집 정상
- Pending Threads: `0`
- Connection Timeout: `0`
- Active Connections: `2`

즉, 기본 JVM/앱 상태 모니터링은 정상이다.

### 5-3. 아직 보강이 필요한 패널

- `RPS`: `No data`
- `API별 응답시간`: `No data`
- `주문 생성 추이`: `No data`

이 말은 “앱 메트릭 수집은 되고 있지만, 일부 비즈니스/HTTP 패널 쿼리는 현재 라벨이나 메트릭 이름과 안 맞을 수 있다”는 뜻이다. 따라서 다음 단계에서는 `8080(prod)` 와 `18080(perf)` 를 구분할 수 있게 Prometheus target 또는 Grafana variable 을 추가하는 편이 좋다.

### 5-4. 운영 이상 징후로 볼 수 있는 부분

- `5xx 에러율`에 작은 스파이크가 있었다.
- `알림 실패(DLQ)` 에도 스파이크가 있었다.

즉, 성능 측정 체계와 별개로 현재 운영 관찰 대시보드에서는 “서버 오류 0” 상태는 아니며, 알림 worker 관련 실패 추적이 추가로 필요하다는 힌트를 준다.

## 6. 로컬 raw 결과 위치

GitHub에는 raw 아티팩트 전체를 올리지 않고, 아래 로컬 파일을 기준으로 숫자만 정리했다.

### 6-1. measure 원본

```text
artifacts/perf/20260415-002837/home-stores/measure/summary.md
artifacts/perf/20260415-002907/orders/measure/summary.md
artifacts/perf/20260415-002908/sales-export/measure/summary.md
artifacts/perf/20260415-002911/owner-menus/measure/summary.md
artifacts/perf/20260415-002942/store-no-show-posts/measure/summary.md
```

### 6-2. smoke 원본

```text
artifacts/perf/20260415-000710/home-stores/smoke/summary.md
artifacts/perf/20260415-000710/orders/smoke/summary.md
artifacts/perf/20260415-000711/sales-export/smoke/summary.md
artifacts/perf/20260415-000714/owner-menus/smoke/summary.md
artifacts/perf/20260415-000714/store-no-show-posts/smoke/summary.md
```

## 7. 이 작업을 한 줄로 설명하면

이번 작업은 “성능을 재보자” 수준이 아니라, 아래까지 포함한 **재현 가능한 성능 측정 체계 구축**이다.

- k6 표준화
- Docker 실행 래퍼
- 성능 전용 SQL seed
- 결과 summary 자동 저장
- 날짜 포맷/스케줄러/운영 실행 경로 불일치 수정
- GitHub에서 바로 읽을 수 있는 결과 보고서 작성

## 8. 다음 단계 권장 사항

1. `medium`, `large` 데이터셋 baseline 추가
- 현재는 `small` 기준선만 정리되어 있다.

2. `8080(prod)` 와 `18080(perf)` 를 Grafana에서 분리
- 운영 대시보드와 성능 측정 대시보드가 섞이면 해석이 흐려진다.

3. `RPS`, `API별 응답시간`, `주문 생성 추이` 패널 쿼리 점검
- 지금은 일부 패널이 `No data` 상태다.

4. 이력서용 숫자는 `measure + medium/large` 기준으로 별도 갱신
- 현재 수치는 1차 기준선으로는 충분하지만, 대표 지표로 쓰려면 데이터 규모를 키우는 편이 좋다.
