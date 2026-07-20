# Store/Menu Image Flow

이 문서는 현재 구현된 이미지 업로드 + 매장 등록 흐름을 0에서 100까지 설명합니다.

## 1. Presign 요청
- 프론트는 `/v1/api/stores/{storeId}/uploads/presign` (또는 메뉴 용 API)로 `fileName`, `contentType`을 보냅니다.
- 서버는 `UploadService#presign`에서 S3Presigner로 PUT URL을 생성합니다.
  - key는 `stores/{referenceId}/{uuid}.{확장자}` 형식입니다.
  - 응답: `{ url, key, expiresInSeconds }`

## 2. S3 업로드
- 프론트는 받은 `url`로 헤더 없이 `PUT` 전송합니다.
- 성공하면 S3 객체가 생성되지만, 아직 DB에는 아무 정보도 저장되지 않습니다.

## 3. 업로드 확인 (Confirm)
- 프론트는 `/uploads/confirm`에 `key`, `etag`를 보내 업로드 성공을 서버에 알립니다.
- `UploadService#confirm`은 현재 다음을 수행합니다.
  1. key prefix 검사 (target + referenceId)
  2. `headObject`로 실제 ETag, Content-Type 조회
  3. ETag/파일 크기 검증
  4. `S3Metadata(key, etag, contentType)` 반환
- **주의**: confirm 단계에서 아직 매장/메뉴 테이블을 업데이트하지는 않습니다. 이 메타데이터를 컨트롤러/서비스가 받아서 등록/수정 API에 전달해야 합니다.

## 4. 매장 등록 / 수정
- `/v1/api/stores` 등록 API 호출 시 `StoreCreateRequest`에 `imageKey`, `imageMime`, `imageEtag`를 포함하면 엔티티에 저장됩니다.
  - 이미지가 있으면 `image_status = uploaded`, 없으면 `pending`.
- `/v1/api/stores/{id}` 수정 API도 동일하게 새로운 이미지 정보를 전달하면 교체합니다.

## 5. 실제 저장 시점
- DB에는 생성/수정 API를 호출한 순간에만 이미지 정보가 저장됩니다.
- Presign/Confirm 단계에서 여러 번 업로드해도 마지막에 API가 전달한 key만 저장됩니다. 이전에 올린 파일은 수동 정리 대상입니다.

## 6. 남은 작업 / 미완성 부분
- Confirm 단계에서 반환한 `S3Metadata`를 Store/Menu 서비스가 받아 자동으로 `image_*` 필드를 갱신하도록 연결되어 있지 않습니다.
- 업로드 중간에 남는 S3 객체를 정리하는 배치나 삭제 로직이 없습니다.
- 소유권 검증(사용자가 해당 매장/메뉴의 주인인지) 로직이 아직 업로드 컨트롤러에 연결되어 있지 않습니다.

이 문서를 참고하여 추후 이미지 등록/수정 기능을 확장하고, 누락된 부분을 보완하면 됩니다.
