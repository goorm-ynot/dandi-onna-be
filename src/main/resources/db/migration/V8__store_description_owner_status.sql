-- Ensure owner profiles are active and describe stores/menus.

ALTER TABLE stores
    ADD COLUMN IF NOT EXISTS description TEXT;

ALTER TABLE menus
    ADD COLUMN IF NOT EXISTS description TEXT;

UPDATE owner_profiles
SET status = TRUE
WHERE user_id IN (
    SELECT id FROM users WHERE role = 'OWNER'
);

WITH descs (login_id, description) AS (
    VALUES
        ('CEO1',  '판교에서 숙성회와 해산물 덮밥을 즐길 수 있는 정통 일식 다이닝.'),
        ('CEO2',  '숯불 향을 입힌 두툼한 모둠회와 계절 안주를 전문으로 하는 선술집.'),
        ('CEO3',  '연어 코스와 사시미를 합리적인 가격으로 제공하는 연어 전문 포차.'),
        ('CEO4',  '동해 직송 활어를 즉시 손질해 내는 막회 전문 매장.'),
        ('CEO5',  '대형 수조를 갖춘 활어 전문점으로 가족 모임과 회식에 적합한 공간.'),
        ('CEO6',  '새벽에 공수한 어패류와 따뜻한 탕요리를 함께 즐길 수 있는 해산물 식당.'),
        ('CEO7',  '경북 포항식 양념과 보리밥이 함께 나오는 지역 특화 막회집.'),
        ('CEO8',  '울진 앞바다에서 들여온 해산물과 물회가 인기인 포장 배송 전문점.'),
        ('CEO9',  '소규모 모임을 위한 프라이빗 해산물 코스를 선보이는 아담한 어촌 콘셉트.'),
        ('CEO10', '광어와 우럭 위주로 숙성도와 커팅을 선택할 수 있는 맞춤형 횟집.'),
        ('CEO11', '정자동 직장인을 위한 빠른 점심 물회와 저녁 회 코스를 제공하는 매장.'),
        ('CEO12', '도매시장 직영을 통해 합리적인 가격의 모둠회와 회덮밥을 선보이는 수산점.'),
        ('CEO13', '참다랑어 전 부위를 맛볼 수 있는 프리미엄 참치 전문 다이닝.'),
        ('CEO14', '목포식 세꼬시와 남도 한정식을 함께 즐길 수 있는 한상 차림.'),
        ('CEO15', '해산물과 한우 모둠을 동시에 맛볼 수 있는 해물 한상 전문점.'),
        ('CEO16', '감성 포차 분위기에서 간장새우와 회무침을 곁들일 수 있는 공간.'),
        ('CEO17', '직접 손질한 두툼한 참치 썰미와 이자카야 안주를 겸한 참치 바.'),
        ('CEO18', '신선한 활어회와 스테이크를 접목한 퓨전 씨푸드 다이닝.'),
        ('CEO19', '물회와 모둠회, 해산물 전골을 즐길 수 있는 지역 주민 사랑방.'),
        ('CEO20', '모란 시장 인근 밤늦게까지 운영되는 서민형 활어회 하우스.'),
        ('CEO21', '여수식 초장과 갓김치가 곁들여진 남해풍 해산물 전문점.'),
        ('CEO22', '깔끔한 2층 홀이 있는 현대적 인테리어의 활어회 레스토랑.')
)
UPDATE stores s
SET description = d.description
FROM users u
JOIN descs d ON u.login_id = d.login_id
WHERE u.id = s.owner_user_id;
