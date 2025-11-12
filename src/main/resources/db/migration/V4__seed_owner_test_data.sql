-- Seed CEO test accounts, owner profiles, and stores with open/close times.
-- Assumes existing schema from V1..V3.

ALTER TABLE owner_profiles
	ALTER COLUMN phone TYPE BIGINT USING phone::bigint;

ALTER TABLE stores
	ALTER COLUMN phone TYPE BIGINT USING phone::bigint,
	ADD COLUMN IF NOT EXISTS open_time TIME NOT NULL DEFAULT TIME '10:00',
	ADD COLUMN IF NOT EXISTS close_time TIME NOT NULL DEFAULT TIME '22:00';

WITH raw (idx, login_id, password_hash, owner_name, owner_phone_raw, store_name, store_phone_raw, address_road, lat, lon, open_txt, close_txt, day_off) AS (
	VALUES
	(1, 'CEO1',  '$2b$12$XXiuDljqFNNix86MqW1vEu.XW6YMQi5hUd7TIYstPFcGmQ4RRXmMm', '김초루', '010-1234-5678', '초루 판교점', '0507-1330-7676', '경기 성남시 분당구 서판교로44번길 3-19 1층 101호', 37.391180, 127.099360, '11:00', '22:00', '없음'),
	(2, 'CEO2',  '$2b$12$O1kEckGtkZe/D7GyKk5qkeRhvAJuD4yfU6kh6rA8CWRw7GM9ZujhW', '이두툼', '010-1234-5679', '두툼해', '0507-1301-2217', '경기 성남시 분당구 판교공원로2길 54-1 1층', 37.390152, 127.092213, '13:00', '23:30', '월요일'),
	(3, 'CEO3',  '$2b$12$YE82Ktbg4kzVtFa.WcsZj.2IFVKYPQVLyXAcanafqOxJiUyjKRXxm', '박연어', '010-1234-5680', '연어의하루 회포차 횟집 회 술집', '0507-1313-7707', '경기 성남시 분당구 운중로 129 마크시티엘로우 106호, 107호', 37.391994, 127.077283, '17:00', '22:00', '없음'),
	(4, 'CEO4',  '$2b$12$.4jDIAWz0E5Iru4mlc5gSeidz9wCxItWmUcnepg4xtNlJ49PCA.E.', '정대진', '010-1234-5679', '대진항막회', '0507-1359-8205', '경기 성남시 분당구 동판교로 91 상가동 1층', 37.391855, 127.117025, '17:00', '23:00', '월요일'),
	(5, 'CEO5',  '$2b$12$0V/XPLw2OY2QGU/w8RUnHeKf6b5C41Dc99j/ubXsXZ3FK/1y3jZ.y', '장진복', '010-1234-5680', '진복호 분당점', '031-709-3382', '경기 성남시 분당구 동판교로 52 제이스타빌딩', 37.387853, 127.114739, '17:00', '23:00', '월요일'),
	(6, 'CEO6',  '$2b$12$pgXjUuWjwDFJqFvPOkr2HesVlKqadAs3RbuIGadu2.7FnWNUDF6LK', '황바다', '010-1234-5681', '바다어장 서현본점', '0507-1378-6907', '경기 성남시 분당구 서현로180번길 29 1층', 37.387447, 127.121396, '10:00', '23:00', '없음'),
	(7, 'CEO7',  '$2b$12$zGI4nGvjWm6G/FYv1yJlP.TwdIypWsU/OrrMLY1hHKBMsJYkeyoq6', '권포항', '010-1234-5680', '포항막회', '0507-1472-1582', '경기 성남시 분당구 정자일로 135 D동 1층 125호', 37.362432, 127.105906, '16:00', '23:30', '일요일'),
	(8, 'CEO8',  '$2b$12$3meYlRgwHslgLyuU.o.XJuVupiIqaytE1qN3uOlvmu5fZbZtdLSpi', '유울진', '010-1234-5681', '울진죽변항', '031-711-6577', '경기 성남시 분당구 정자일로6번길 13 1층', 37.351003, 127.108189, '14:30', '23:00', '없음'),
	(9, 'CEO9',  '$2b$12$q3SJJ837ceXtJfNLWBvMgu43fYkDcBQsFt01NKe.6ZjnO7eMu5UfK', '배작은', '010-1234-5682', '작은어촌', '031-719-4977', '경기 성남시 분당구 미금일로74번길 29-4', 37.348673, 127.111532, '15:00', '23:00', '일요일'),
	(10,'CEO10', '$2b$12$p1GKFGgIouTXWHW6w/6XlO/3WFz.G2NWi.bvjccW2BGwIBqnJ85nC', '최광어', '010-1234-5681', '광어야왜우럭 오리역점', '0507-1462-7920', '경기 성남시 분당구 구미로9번길 3-4 1층 일부호', 37.338834, 127.109796, '11:30', '22:00', '없음'),
	(11,'CEO11', '$2b$12$RnQjKF5CSXxYkBwlzukXReJqYVI.t8qgzVF97cBGEJRl0R44AipUW', '이물고', '010-1234-5682', '물고기자리 정자점', '0507-1380-2011', '경기 성남시 분당구 정자일로 120 1층 111호', 37.361014, 127.106576, '15:00', '23:00', '없음'),
	(12,'CEO12', '$2b$12$fUYq4AK6.Om27lFohNyojec0L8IDyGsQGMCzkNs9Odyf5CRUJWKMu', '김팔딱', '010-1234-5683', '팔딱수산', '0507-1408-1047', '경기 성남시 분당구 대왕판교로606번길 10 타워1동 1층 115호', 37.395640, 127.108910, '05:00', '23:00', '일요일'),
	(13,'CEO13', '$2b$12$KBVPHf.NN3TqMirj6WCSu.DhR3f/nPzh2AViZyW60tLif3V94a5ce', '최도원', '010-1234-5682', '도원참치 판교본점', '031-8016-4588', '경기 성남시 분당구 판교역로192번길 14-2 골드타워 2층 도원참치', 37.397570, 127.112020, '11:00', '23:00', '일요일'),
	(14,'CEO14', '$2b$12$rs703yYHsGsx7aIgt3uuReKQ.x58XORF/mIjINmWSkyd3M8KaCqla', '고목포', '010-1234-6682', '목포명가 판교점', '031-707-6233', '경기 성남시 분당구 분당내곡로 159 메디피움 A동 1층', 37.398090, 127.112552, '11:30', '23:30', '일요일'),
	(15,'CEO15', '$2b$12$x54Lbebhgf3iZd0z6/kKG.gDPu.r2Lz8k0VTPI7A1I/XhSR8QHqfa', '하해미', '010-1234-7682', '해미옥 판교본점', '0507-1372-0732', '경기 성남시 분당구 대왕판교로606번길 41 지하 1층 B01호', 37.396640, 127.111950, '11:00', '23:00', '일요일'),
	(16,'CEO16', '$2b$12$sqiedDDVusbUhgZDyat4LOeRk3h1NoHKSaYxayo1jxyuDiltNy5Xe', '육동해', '010-1234-8682', '동해회포차', '031-8016-3995', '경기 성남시 분당구 대왕판교로606번길 31', 37.396950, 127.110470, '15:00', '23:00', '일요일'),
	(17,'CEO17', '$2b$12$1oUCAqWZdNprV5uP7EdY.uqrbklaWVztDfuFJD8hqCnMFHh4X0dnu', '나달인', '010-1234-8683', '달인참치 야탑점', '031-706-6400', '경기 성남시 분당구 야탑로105번길 22-5', 37.411730, 127.130900, '15:00', '23:00', '없음'),
	(18,'CEO18', '$2b$12$zZxAPpoF035kQcOKg59w1.ObKFh.Ax7n/ocpmNq.WOPl7OEDTJ//S', '도도심', '010-1234-8684', '도심속바다', '031-552-3987', '경기 성남시 분당구 장미로92번길 12', 37.412780, 127.130730, '15:00', '23:00', '없음'),
	(19,'CEO19', '$2b$12$.f3H6kEWgZV2TLb6SXyk7uicV2G6vRsDrY1ECtmpPKte2Kk2pLjem', '연어가', '010-1234-8685', '어가', '031-854-6624', '경기 성남시 중원구 성남대로1130번길 7 1층 102호', 37.431080, 127.130350, '17:00', '23:00', '일요일'),
	(20,'CEO20', '$2b$12$xhGClKfqXi56Qh4xnSBJ/u.4oHKNZGHYt6z0yUGGyYqMGMNz7bVXq', '숫총각', '010-1234-8686', '총각네횟집 모란오거리점', '0507-1401-5849', '경기 성남시 중원구 성남대로1148번길 13 총각네 횟집', 37.432850, 127.130730, '11:00', '23:00', '없음'),
	(21,'CEO21', '$2b$12$9fwAuyIG3x.t2Ss3Q9ZopeNG3md5U44Mur74MWXTEDtP3s8N2cW/y', '일이육', '010-1234-8687', '26여수물고기', '0507-1310-7726', '경기 성남시 중원구 제일로63번길 28-1 1층', 37.432120, 127.132620, '12:00', '23:00', '없음'),
	(22,'CEO22', '$2b$12$.y2lrh3nFD6Eqx5Ul5E8subTc1q0Ibj4QVLXHc3te0O4ZZGD17VE.', '싱싱회', '010-1234-8688', '이든활어', '0507-1328-8663', '경기 성남시 분당구 대왕판교로 670 2층 220호', 37.402228, 127.106946, '11:30', '22:00', '없음')
),

normalized AS (
	SELECT
		idx,
		login_id,
		password_hash,
		owner_name,
		regexp_replace(owner_phone_raw, '[^0-9]', '', 'g')::bigint AS owner_phone,
		store_name,
		regexp_replace(store_phone_raw, '[^0-9]', '', 'g')::bigint AS store_phone,
		address_road,
		lat,
		lon,
		open_txt::time  AS open_time,
		close_txt::time AS close_time,
		day_off
	FROM raw
),

inserted_users AS (
	INSERT INTO users (id, login_id, password_hash, role)
	SELECT gen_random_uuid(), login_id, password_hash, 'OWNER'
	FROM normalized
	RETURNING id, login_id
),

joined AS (
	SELECT u.id AS user_id, n.*
	FROM normalized n
	JOIN inserted_users u ON u.login_id = n.login_id
),

insert_profiles AS (
	INSERT INTO owner_profiles (user_id, name, phone, status)
	SELECT user_id, owner_name, owner_phone, TRUE
	FROM joined
),

insert_stores AS (
	INSERT INTO stores (id, owner_user_id, name, phone, address_road, lat, lon, open_time, close_time)
	SELECT gen_random_uuid(), user_id, store_name, store_phone, address_road, lat, lon, open_time, close_time
	FROM joined
)
SELECT 1;
