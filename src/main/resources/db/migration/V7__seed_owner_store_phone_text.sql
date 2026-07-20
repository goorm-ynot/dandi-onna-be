WITH raw (
    login_id,
    password_hash,
    owner_name,
    owner_phone,
    store_name,
    store_phone,
    address_road,
    lat,
    lon,
    open_txt,
    close_txt,
    day_off
) AS (
    VALUES
    ('CEO1','$2b$12$ZaLpWivbBI/QYy/gJaWnnuy3cidHujruSWLly1zBgSrgIxTc.mVNy','김초루','010-1234-5678','초루 판교점','0507-1330-7676','경기 성남시 분당구 서판교로44번길 3-19 1층 101호',37.39118,127.09936,'11:00','22:00','없음'),
    ('CEO2','$2b$12$EksbaLgeV1WzWsKhTD/pS.eQaBighWeH55W0EI1lPQQWdSYaoIuqq','이두툼','010-1234-5679','두툼해','0507-1301-2217','경기 성남시 분당구 판교공원로2길 54-1 1층',37.390152,127.092213,'13:00','23:30','월요일'),
    ('CEO3','$2b$12$aMxEVsviXcjxUcH.MucZ2up6Pq3FzJxlmI/YNnplzA.5x7ObAMcW6','박연어','010-1234-5680','연어의하루 회포차 횟집 회 술집','0507-1313-7707','경기 성남시 분당구 운중로 129 마크시티엘로우 106호 107호',37.391994,127.077283,'17:00','22:00','없음'),
    ('CEO4','$2b$12$DRAwYbMzbRMIgYHZ0rbuJuE3aG8YA90oYk8lBG.uXCBrVhIYmLSYy','정대진','010-1234-5679','대진항막회','0507-1359-8205','경기 성남시 분당구 동판교로 91 상가동 1층',37.391855,127.117025,'17:00','23:00','월요일'),
    ('CEO5','$2b$12$SWMczwi0XhTPmuRmu./EW.W4in4HivXohPRnloWJMsSvTQ4SqKHyG','장진복','010-1234-5680','진복호 분당점','031-709-3382','경기 성남시 분당구 동판교로 52 제이스타빌딩',37.387853,127.114739,'17:00','23:00','월요일'),
    ('CEO6','$2b$12$K7xvJ9r80zYEbkdkixUnAeuBf2Vz56wvwDnAySBtEFNjdG6Dn61Rm','황바다','010-1234-5681','바다어장 서현본점','0507-1378-6907','경기 성남시 분당구 서현로180번길 29 1층',37.387447,127.121396,'10:00','23:00','없음'),
    ('CEO7','$2b$12$VQHNsJpH.Glgf3RXVmo6UebPj.2p2XYLOh2te2RhVodr..ue3FouW','권포항','010-1234-5680','포항막회','0507-1472-1582','경기 성남시 분당구 정자일로 135 D동 1층 125호',37.362432,127.105906,'16:00','23:30','일요일'),
    ('CEO8','$2b$12$HPuR1cNmGJKXtBBOypXR9OFOyHTsrfd3ZupZjPvb2VJztWRijjcDy','유울진','010-1234-5681','울진죽변항','031-711-6577','경기 성남시 분당구 정자일로6번길 13 1층',37.351003,127.108189,'14:30','23:00','없음'),
    ('CEO9','$2b$12$c3hrn1B03fV/.BYbBGImIOjbXg3ZZxT2F.TdLJ0fjsgYrAsDIXvQO','배작은','010-1234-5682','작은어촌','031-719-4977','경기 성남시 분당구 미금일로74번길 29-4',37.348673,127.111532,'15:00','23:00','일요일'),
    ('CEO10','$2b$12$Sf8byES/KfEvXZhLJPAy8eL1sqtaAHA1VMsGBig36gpx5q32FsBo2','최광어','010-1234-5681','광어야왜우럭 오리역점','0507-1462-7920','경기 성남시 분당구 구미로9번길 3-4 1층 일부호',37.338834,127.109796,'11:30','22:00','없음'),
    ('CEO11','$2b$12$Oj4N7EZkrcW7TD1g/n.jLu6Qa08TWMwzgVqkL3tUVcsBhfA130grS','이물고','010-1234-5682','물고기자리 정자점','0507-1380-2011','경기 성남시 분당구 정자일로 120 1층 111호',37.361014,127.106576,'15:00','23:00','없음'),
    ('CEO12','$2b$12$X1lwivMjOz/yit.sHS2zQOhBmiPt1XhMI9cWZ0irQmbjNMjzOHH2y','김팔딱','010-1234-5683','팔딱수산','0507-1408-1047','경기 성남시 분당구 대왕판교로606번길 10 타워1동 1층 115호',37.39564,127.10891,'05:00','23:00','일요일'),
    ('CEO13','$2b$12$cqTH5QztgUHg3hFKEEBdW.FNQnYYX4mjFObkLpNkLt8GWJ0RuPuMG','최도원','010-1234-5682','도원참치 판교본점','031-8016-4588','경기 성남시 분당구 판교역로192번길 14-2 골드타워 2층 도원참치',37.39757,127.11202,'11:00','23:00','일요일'),
    ('CEO14','$2b$12$6lR7yvDxHx4iuo2LgGHTle.THBwUGtQlFjWziidk.DDALB.Efa9b.','고목포','010-1234-6682','목포명가 판교점','031-707-6233','경기 성남시 분당구 분당내곡로 159 메디피움 A동 1층',37.39809,127.112552,'11:30','23:30','일요일'),
    ('CEO15','$2b$12$iDlofCQqxt69A3qaZtQ4HOttOp3Zb4XnhLtUeODCNENkhfpjSkM0S','하해미','010-1234-7682','해미옥 판교본점','0507-1372-0732','경기 성남시 분당구 대왕판교로606번길 41 지하 1층 B01호',37.39664,127.11195,'11:00','23:00','일요일'),
    ('CEO16','$2b$12$9eduz1u2YLIhUElzI3uAD.UDWsMq0yqS3DOy1U7FG2I50mEI6kQBK','육동해','010-1234-8682','동해회포차','031-8016-3995','경기 성남시 분당구 대왕판교로606번길 31',37.39695,127.11047,'15:00','23:00','일요일'),
    ('CEO17','$2b$12$7QVuKNPltBqYsag3DJGl0O7Zo86ocpdMDqqSImeQPA3RYqidC1bDO','나달인','010-1234-8683','달인참치 야탑점','031-706-6400','경기 성남시 분당구 야탑로105번길 22-5',37.41173,127.1309,'15:00','23:00','없음'),
    ('CEO18','$2b$12$7vBeqSbKnUZxpkPySGqCuOipC2HVeilJob55ooolhgpBXMmBBdDIC','도도심','010-1234-8684','도심속바다','031-552-3987','경기 성남시 분당구 장미로92번길 12',37.41278,127.13073,'15:00','23:00','없음'),
    ('CEO19','$2b$12$sqk6nzDoTPAgWKPDDYiazOwfLV08QcLHRacQC33Mw53uBi2ndDQt.','연어가','010-1234-8685','어가','031-854-6624','경기 성남시 중원구 성남대로1130번길 7 1층 102호',37.43108,127.13035,'17:00','23:00','일요일'),
    ('CEO20','$2b$12$Lm1TLaiaRTWiy09lfRXJ4eGKEnRRepir1vNv29udCjY3VtUK64diu','숫총각','010-1234-8686','총각네횟집 모란오거리점','0507-1401-5849','경기 성남시 중원구 성남대로1148번길 13 총각네 횟집',37.43285,127.13073,'11:00','23:00','없음'),
    ('CEO21','$2b$12$/Cz6dtZcPfeOi44JSe.uO.m1NnIz1LDOKT6epXp6mkzmfNRHtspgO','일이육','010-1234-8687','26여수물고기','0507-1310-7726','경기 성남시 중원구 제일로63번길 28-1 1층',37.43212,127.13262,'12:00','23:00','없음'),
    ('CEO22','$2b$12$ody1mLu0KmREKpsOgaGbHOCOmRDoTlDvnonXdf5pUMKEswne/QWim','싱싱회','010-1234-8688','이든활어','0507-1328-8663','경기 성남시 분당구 대왕판교로 670 2층 220호',37.402228,127.106946,'11:30','22:00','없음')
),
users AS (
    INSERT INTO users (id, login_id, password_hash, role)
    SELECT gen_random_uuid(), login_id, password_hash, 'OWNER'
    FROM raw
    RETURNING id, login_id
),
joined AS (
    SELECT u.id AS user_id, r.*
    FROM raw r
    JOIN users u ON u.login_id = r.login_id
),
profiles AS (
    INSERT INTO owner_profiles (user_id, name, phone, status)
    SELECT user_id, owner_name, owner_phone, TRUE
    FROM joined
    RETURNING user_id
),
stores AS (
    INSERT INTO stores (id, owner_user_id, name, phone, address_road, lat, lon, open_time, close_time)
    SELECT gen_random_uuid(), user_id, store_name, store_phone, address_road, lat, lon,
        open_txt::time, close_txt::time
    FROM joined
    RETURNING id, owner_user_id
),
info AS (
    SELECT s.id AS store_id, j.open_txt, j.close_txt, j.day_off
    FROM stores s
    JOIN joined j ON j.user_id = s.owner_user_id
)
INSERT INTO store_hours (store_id, dow, seq, open_time, close_time)
SELECT store_id, dow, 1, open_time, close_time
FROM (
    SELECT store_id,
        CASE day_off
            WHEN '일요일' THEN 0
            WHEN '월요일' THEN 1
            WHEN '화요일' THEN 2
            WHEN '수요일' THEN 3
            WHEN '목요일' THEN 4
            WHEN '금요일' THEN 5
            WHEN '토요일' THEN 6
            ELSE NULL
        END AS off_dow,
        open_txt::time AS open_time,
        close_txt::time AS close_time
    FROM info
) base
CROSS JOIN LATERAL generate_series(0, 6) AS dow
WHERE base.off_dow IS NULL OR base.off_dow <> dow;
