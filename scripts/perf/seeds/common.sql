CREATE OR REPLACE FUNCTION perf_seed_reset()
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
	DELETE FROM export_jobs
	WHERE store_id IN (
		SELECT s.id
		FROM stores s
		JOIN users u ON u.id = s.owner_user_id
		WHERE u.login_id LIKE 'PERF_%'
	)
	   OR requested_by IN (SELECT id FROM users WHERE login_id LIKE 'PERF_%');

	DELETE FROM no_show_post_schedule_items
	WHERE schedule_id IN (
		SELECT id
		FROM no_show_post_schedules
		WHERE store_id IN (
			SELECT s.id
			FROM stores s
			JOIN users u ON u.id = s.owner_user_id
			WHERE u.login_id LIKE 'PERF_%'
		)
		   OR requested_by IN (SELECT id FROM users WHERE login_id LIKE 'PERF_%')
	);

	DELETE FROM no_show_post_schedules
	WHERE store_id IN (
		SELECT s.id
		FROM stores s
		JOIN users u ON u.id = s.owner_user_id
		WHERE u.login_id LIKE 'PERF_%'
	)
	   OR requested_by IN (SELECT id FROM users WHERE login_id LIKE 'PERF_%');

	DELETE FROM no_show_presets
	WHERE store_id IN (
		SELECT s.id
		FROM stores s
		JOIN users u ON u.id = s.owner_user_id
		WHERE u.login_id LIKE 'PERF_%'
	);

	DELETE FROM favorites
	WHERE store_id IN (
		SELECT s.id
		FROM stores s
		JOIN users u ON u.id = s.owner_user_id
		WHERE u.login_id LIKE 'PERF_%'
	)
	   OR consumer_user_id IN (SELECT id FROM users WHERE login_id LIKE 'PERF_%');

	DELETE FROM notification_targets
	WHERE user_id IN (SELECT id FROM users WHERE login_id LIKE 'PERF_%')
	   OR notification_id IN (
			SELECT id
			FROM notifications
			WHERE created_by IN (SELECT id FROM users WHERE login_id LIKE 'PERF_%')
	   );

	DELETE FROM notifications
	WHERE created_by IN (SELECT id FROM users WHERE login_id LIKE 'PERF_%');

	DELETE FROM no_show_order_items
	WHERE order_id IN (
		SELECT id
		FROM no_show_orders
		WHERE store_id IN (
			SELECT s.id
			FROM stores s
			JOIN users u ON u.id = s.owner_user_id
			WHERE u.login_id LIKE 'PERF_%'
		)
	);

	DELETE FROM no_show_orders
	WHERE store_id IN (
		SELECT s.id
		FROM stores s
		JOIN users u ON u.id = s.owner_user_id
		WHERE u.login_id LIKE 'PERF_%'
	);

	DELETE FROM no_show_post_history
	WHERE store_id IN (
		SELECT s.id
		FROM stores s
		JOIN users u ON u.id = s.owner_user_id
		WHERE u.login_id LIKE 'PERF_%'
	);

	DELETE FROM no_show_posts
	WHERE store_id IN (
		SELECT s.id
		FROM stores s
		JOIN users u ON u.id = s.owner_user_id
		WHERE u.login_id LIKE 'PERF_%'
	);

	DELETE FROM menu_set_items
	WHERE set_menu_id IN (
		SELECT m.id
		FROM menus m
		JOIN stores s ON s.id = m.store_id
		JOIN users u ON u.id = s.owner_user_id
		WHERE u.login_id LIKE 'PERF_%'
	)
	   OR component_menu_id IN (
		SELECT m.id
		FROM menus m
		JOIN stores s ON s.id = m.store_id
		JOIN users u ON u.id = s.owner_user_id
		WHERE u.login_id LIKE 'PERF_%'
	);

	DELETE FROM menus
	WHERE store_id IN (
		SELECT s.id
		FROM stores s
		JOIN users u ON u.id = s.owner_user_id
		WHERE u.login_id LIKE 'PERF_%'
	);

	DELETE FROM store_hours
	WHERE store_id IN (
		SELECT s.id
		FROM stores s
		JOIN users u ON u.id = s.owner_user_id
		WHERE u.login_id LIKE 'PERF_%'
	);

	DELETE FROM stores
	WHERE owner_user_id IN (SELECT id FROM users WHERE login_id LIKE 'PERF_%');

	DELETE FROM owner_profiles
	WHERE user_id IN (
		SELECT id
		FROM users
		WHERE login_id LIKE 'PERF_%'
		  AND role = 'OWNER'
	);

	DELETE FROM consumer_profiles
	WHERE user_id IN (
		SELECT id
		FROM users
		WHERE login_id LIKE 'PERF_%'
		  AND role = 'CONSUMER'
	);

	DELETE FROM users
	WHERE login_id LIKE 'PERF_%';
END;
$$;

CREATE OR REPLACE FUNCTION perf_seed_dataset(
	total_store_count integer,
	main_menu_count integer,
	main_open_post_count integer,
	export_order_count integer
)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
	consumer_password_hash constant text := '$2y$10$HpMsB1IR/9CQuxPrS/l5B.bmDJz2q1onSKui5BCu0gtVEqxOrpRO2';
	owner_password_hash constant text := '$2y$10$HpMsB1IR/9CQuxPrS/l5B.bmDJz2q1onSKui5BCu0gtVEqxOrpRO2';
	consumer_id uuid;
	main_owner_id uuid;
	main_store_id uuid;
	aux_store_count integer;
	set_menu_count integer;
	single_menu_count integer;
	seed_now timestamptz := date_trunc('minute', now());
BEGIN
	IF total_store_count < 1 THEN
		RAISE EXCEPTION 'total_store_count must be >= 1';
	END IF;
	IF main_open_post_count < 2 THEN
		RAISE EXCEPTION 'main_open_post_count must be >= 2';
	END IF;
	IF main_menu_count < main_open_post_count + 1 THEN
		RAISE EXCEPTION 'main_menu_count must be at least main_open_post_count + 1';
	END IF;

	PERFORM perf_seed_reset();

	aux_store_count := GREATEST(total_store_count - 1, 0);
	set_menu_count := GREATEST(1, LEAST(main_menu_count / 10, main_menu_count - main_open_post_count));
	single_menu_count := main_menu_count - set_menu_count;

	INSERT INTO users (login_id, password_hash, role)
	VALUES ('Customer1', consumer_password_hash, 'CONSUMER')
	ON CONFLICT (login_id) DO UPDATE
	SET password_hash = EXCLUDED.password_hash,
		role = EXCLUDED.role,
		updated_at = now()
	RETURNING id INTO consumer_id;

	INSERT INTO consumer_profiles (user_id, name, phone, terms_agreed, terms_version)
	VALUES (consumer_id, 'Customer1', '010-1111-2222', TRUE, 'v1')
	ON CONFLICT (user_id) DO UPDATE
	SET name = EXCLUDED.name,
		phone = EXCLUDED.phone,
		terms_agreed = EXCLUDED.terms_agreed,
		terms_version = EXCLUDED.terms_version,
		updated_at = now();

	INSERT INTO users (login_id, password_hash, role)
	VALUES ('PERF_OWNER_MAIN', owner_password_hash, 'OWNER')
	RETURNING id INTO main_owner_id;

	INSERT INTO owner_profiles (user_id, name, phone, status)
	VALUES (main_owner_id, 'PERF Main Owner', '010-9000-0000', TRUE);

	INSERT INTO stores (
		id,
		owner_user_id,
		name,
		category,
		phone,
		address_road,
		lat,
		lon,
		open_time,
		close_time,
		description,
		image_key,
		image_mime,
		image_etag,
		image_status
	)
	VALUES (
		gen_random_uuid(),
		main_owner_id,
		'PERF Main Store',
		'요식업',
		'0507-9000-0000',
		'경기 성남시 분당구 판교역로 235 PERF Main',
		37.394000,
		127.110000,
		TIME '09:00',
		TIME '23:00',
		'성능 측정용 메인 매장',
		'stores/perf-main-store/cover.jpg',
		'image/jpeg',
		'etag-perf-main-store',
		'uploaded'
	)
	RETURNING id INTO main_store_id;

	INSERT INTO no_show_presets (
		store_id,
		name,
		discount_percent,
		visit_available_minutes,
		sale_delay_minutes,
		is_default,
		active
	)
	VALUES (
		main_store_id,
		'PERF Default Preset',
		40,
		30,
		0,
		TRUE,
		TRUE
	);

	DROP TABLE IF EXISTS perf_tmp_main_single_menus;
	CREATE TEMP TABLE perf_tmp_main_single_menus (
		rn integer PRIMARY KEY,
		menu_id uuid NOT NULL,
		menu_name text NOT NULL,
		original_price integer NOT NULL
	) ON COMMIT DROP;

	DROP TABLE IF EXISTS perf_tmp_main_set_menus;
	CREATE TEMP TABLE perf_tmp_main_set_menus (
		rn integer PRIMARY KEY,
		menu_id uuid NOT NULL,
		menu_name text NOT NULL
	) ON COMMIT DROP;

	DROP TABLE IF EXISTS perf_tmp_main_posts;
	CREATE TEMP TABLE perf_tmp_main_posts (
		rn integer PRIMARY KEY,
		post_id bigint NOT NULL,
		menu_id uuid NOT NULL,
		menu_name text NOT NULL,
		original_price integer NOT NULL,
		discounted_price integer NOT NULL,
		discount_percent integer NOT NULL,
		expire_at timestamptz NOT NULL
	) ON COMMIT DROP;

	DROP TABLE IF EXISTS perf_tmp_export_orders;
	CREATE TEMP TABLE perf_tmp_export_orders (
		rn integer PRIMARY KEY,
		order_id uuid NOT NULL,
		order_no text NOT NULL,
		created_at timestamptz NOT NULL,
		visit_time timestamptz NOT NULL
	) ON COMMIT DROP;

	WITH single_source AS (
		SELECT
			gs AS rn,
			CASE
				WHEN gs = 1 THEN 'PERF Expiry Probe'
				ELSE format('PERF Single %s', lpad(gs::text, 4, '0'))
			END AS menu_name,
			8000 + ((gs - 1) % 15) * 500 AS price_krw,
			CASE
				WHEN gs <= (single_menu_count - GREATEST(single_menu_count / 10, 1)) THEN 'on_sale'::menu_status
				ELSE 'sold_out'::menu_status
			END AS status,
			CASE
				WHEN gs % 2 = 0 THEN format('menus/perf-main/single-%s.jpg', lpad(gs::text, 4, '0'))
				ELSE NULL
			END AS image_key
		FROM generate_series(1, single_menu_count) gs
	),
	inserted_singles AS (
		INSERT INTO menus (
			store_id,
			name,
			description,
			price_krw,
			status,
			type,
			image_key,
			image_mime,
			image_etag,
			image_status
		)
		SELECT
			main_store_id,
			menu_name,
			format('성능 측정용 단품 메뉴 %s', rn),
			price_krw,
			status,
			'single',
			image_key,
			CASE WHEN image_key IS NOT NULL THEN 'image/jpeg' ELSE NULL END,
			CASE WHEN image_key IS NOT NULL THEN format('etag-main-single-%s', lpad(rn::text, 4, '0')) ELSE NULL END,
			CASE WHEN image_key IS NOT NULL THEN 'uploaded'::menu_image_status ELSE 'pending'::menu_image_status END
		FROM single_source
		RETURNING id, name
	)
	INSERT INTO perf_tmp_main_single_menus (rn, menu_id, menu_name, original_price)
	SELECT s.rn, i.id, s.menu_name, s.price_krw
	FROM single_source s
	JOIN inserted_singles i ON i.name = s.menu_name;

	WITH set_source AS (
		SELECT
			gs AS rn,
			format('PERF Set %s', lpad(gs::text, 4, '0')) AS menu_name,
			20000 + gs * 500 AS price_krw
		FROM generate_series(1, set_menu_count) gs
	),
	inserted_sets AS (
		INSERT INTO menus (
			store_id,
			name,
			description,
			price_krw,
			status,
			type,
			image_key,
			image_mime,
			image_etag,
			image_status
		)
		SELECT
			main_store_id,
			menu_name,
			format('성능 측정용 세트 메뉴 %s', rn),
			price_krw,
			'on_sale',
			'set',
			CASE WHEN rn % 2 = 0 THEN format('menus/perf-main/set-%s.jpg', lpad(rn::text, 4, '0')) ELSE NULL END,
			CASE WHEN rn % 2 = 0 THEN 'image/jpeg' ELSE NULL END,
			CASE WHEN rn % 2 = 0 THEN format('etag-main-set-%s', lpad(rn::text, 4, '0')) ELSE NULL END,
			CASE WHEN rn % 2 = 0 THEN 'uploaded'::menu_image_status ELSE 'pending'::menu_image_status END
		FROM set_source
		RETURNING id, name
	)
	INSERT INTO perf_tmp_main_set_menus (rn, menu_id, menu_name)
	SELECT s.rn, i.id, s.menu_name
	FROM set_source s
	JOIN inserted_sets i ON i.name = s.menu_name;

	INSERT INTO menu_set_items (set_menu_id, component_menu_id, quantity)
	SELECT
		sm.menu_id,
		c1.menu_id,
		1
	FROM perf_tmp_main_set_menus sm
	JOIN perf_tmp_main_single_menus c1
	  ON c1.rn = ((sm.rn - 1) * 2 % single_menu_count) + 1
	UNION ALL
	SELECT
		sm.menu_id,
		c2.menu_id,
		1
	FROM perf_tmp_main_set_menus sm
	JOIN perf_tmp_main_single_menus c2
	  ON c2.rn = (((sm.rn - 1) * 2 + 1) % single_menu_count) + 1;

	WITH post_source AS (
		SELECT
			sm.rn,
			sm.menu_id,
			sm.menu_name,
			sm.original_price,
			40 AS discount_percent,
			round(sm.original_price * 0.6) AS discounted_price,
			CASE
				WHEN sm.rn = 1 THEN seed_now + interval '1 minute'
				ELSE seed_now + interval '90 minutes'
			END AS expire_at,
			CASE
				WHEN sm.rn = 1 THEN 5
				ELSE 1000
			END AS qty_total
		FROM perf_tmp_main_single_menus sm
		WHERE sm.rn <= main_open_post_count
	),
	inserted_posts AS (
		INSERT INTO no_show_posts (
			store_id,
			menu_id,
			price_percent,
			discounted_unit_price,
			original_unit_price,
			qty_total,
			qty_remaining,
			start_at,
			expire_at,
			status
		)
		SELECT
			main_store_id,
			menu_id,
			discount_percent,
			discounted_price,
			original_price,
			qty_total,
			qty_total,
			seed_now - interval '15 minutes',
			expire_at,
			'open'
		FROM post_source
		RETURNING id, menu_id
	)
	INSERT INTO perf_tmp_main_posts (
		rn,
		post_id,
		menu_id,
		menu_name,
		original_price,
		discounted_price,
		discount_percent,
		expire_at
	)
	SELECT
		ps.rn,
		ip.id,
		ps.menu_id,
		ps.menu_name,
		ps.original_price,
		ps.discounted_price,
		ps.discount_percent,
		ps.expire_at
	FROM post_source ps
	JOIN inserted_posts ip ON ip.menu_id = ps.menu_id;

	INSERT INTO favorites (consumer_user_id, store_id)
	VALUES (consumer_id, main_store_id);

	IF aux_store_count > 0 THEN
		WITH aux_source AS (
			SELECT
				gs AS n,
				format('PERF_OWNER_%s', lpad(gs::text, 5, '0')) AS login_id,
				format('PERF Store %s', lpad(gs::text, 5, '0')) AS store_name,
				format('PERF Aux Owner %s', lpad(gs::text, 5, '0')) AS owner_name,
				format('010-92%s', lpad(gs::text, 6, '0')) AS phone,
				37.394000 + ((gs % 100)::numeric / 10000) AS lat,
				127.110000 + ((gs / 100)::numeric / 10000) AS lon
			FROM generate_series(1, aux_store_count) gs
		),
		inserted_aux_users AS (
			INSERT INTO users (login_id, password_hash, role)
			SELECT login_id, owner_password_hash, 'OWNER'
			FROM aux_source
			RETURNING id, login_id
		),
		aux_joined AS (
			SELECT
				s.n,
				s.store_name,
				s.owner_name,
				s.phone,
				s.lat,
				s.lon,
				u.id AS owner_user_id
			FROM aux_source s
			JOIN inserted_aux_users u ON u.login_id = s.login_id
		),
		inserted_profiles AS (
			INSERT INTO owner_profiles (user_id, name, phone, status)
			SELECT owner_user_id, owner_name, phone, TRUE
			FROM aux_joined
			RETURNING user_id
		),
		inserted_stores AS (
			INSERT INTO stores (
				id,
				owner_user_id,
				name,
				category,
				phone,
				address_road,
				lat,
				lon,
				open_time,
				close_time,
				description,
				image_key,
				image_mime,
				image_etag,
				image_status
			)
			SELECT
				gen_random_uuid(),
				owner_user_id,
				store_name,
				'요식업',
				phone,
				format('경기 성남시 분당구 PERF Aux %s', lpad(n::text, 5, '0')),
				lat,
				lon,
				TIME '09:00',
				TIME '23:00',
				'성능 측정용 보조 매장',
				CASE WHEN n % 3 = 0 THEN format('stores/perf-aux/%s.jpg', lpad(n::text, 5, '0')) ELSE NULL END,
				CASE WHEN n % 3 = 0 THEN 'image/jpeg' ELSE NULL END,
				CASE WHEN n % 3 = 0 THEN format('etag-store-%s', lpad(n::text, 5, '0')) ELSE NULL END,
				CASE WHEN n % 3 = 0 THEN 'uploaded'::menu_image_status ELSE 'pending'::menu_image_status END
			FROM aux_joined
			RETURNING id, owner_user_id
		),
		aux_store_rows AS (
			SELECT
				j.n,
				s.id AS store_id
			FROM aux_joined j
			JOIN inserted_stores s ON s.owner_user_id = j.owner_user_id
		),
		inserted_aux_menus AS (
			INSERT INTO menus (
				store_id,
				name,
				description,
				price_krw,
				status,
				type,
				image_key,
				image_mime,
				image_etag,
				image_status
			)
			SELECT
				store_id,
				format('PERF Aux Menu %s', lpad(n::text, 5, '0')),
				'성능 측정용 보조 메뉴',
				9000 + (n % 10) * 500,
				'on_sale',
				'single',
				CASE WHEN n % 2 = 0 THEN format('menus/perf-aux/%s.jpg', lpad(n::text, 5, '0')) ELSE NULL END,
				CASE WHEN n % 2 = 0 THEN 'image/jpeg' ELSE NULL END,
				CASE WHEN n % 2 = 0 THEN format('etag-menu-%s', lpad(n::text, 5, '0')) ELSE NULL END,
				CASE WHEN n % 2 = 0 THEN 'uploaded'::menu_image_status ELSE 'pending'::menu_image_status END
			FROM aux_store_rows
			RETURNING id, store_id
		)
		INSERT INTO no_show_posts (
			store_id,
			menu_id,
			price_percent,
			discounted_unit_price,
			original_unit_price,
			qty_total,
			qty_remaining,
			start_at,
			expire_at,
			status
		)
		SELECT
			im.store_id,
			im.id,
			35,
			6500,
			10000,
			100,
			100,
			seed_now - interval '10 minutes',
			seed_now + interval '120 minutes' + make_interval(secs => (row_number() OVER (ORDER BY im.store_id))::integer % 600),
			'open'
		FROM inserted_aux_menus im;
	END IF;

	WITH order_source AS (
		SELECT
			gs AS rn,
			p1.post_id AS first_post_id,
			p1.menu_id AS first_menu_id,
			p1.menu_name AS first_menu_name,
			p1.original_price AS first_original_price,
			p1.discounted_price AS first_discounted_price,
			p1.discount_percent AS discount_percent,
			p2.post_id AS second_post_id,
			p2.menu_id AS second_menu_id,
			p2.menu_name AS second_menu_name,
			p2.original_price AS second_original_price,
			p2.discounted_price AS second_discounted_price,
			seed_now
				- make_interval(days => ((gs - 1) % 28) + 1)
				- make_interval(hours => (gs % 10), mins => (gs % 50)) AS created_at,
			seed_now
				- make_interval(days => ((gs - 1) % 28) + 1)
				+ interval '2 hours' AS visit_time,
			format('NS-%s-P%s',
				to_char(seed_now - make_interval(days => ((gs - 1) % 28) + 1), 'YYYYMMDD'),
				lpad(gs::text, 6, '0')
			) AS order_no
		FROM generate_series(1, export_order_count) gs
		JOIN perf_tmp_main_posts p1 ON p1.rn = ((gs - 1) % GREATEST(main_open_post_count - 1, 1)) + 2
		JOIN perf_tmp_main_posts p2 ON p2.rn = (gs % GREATEST(main_open_post_count - 1, 1)) + 2
	),
	inserted_orders AS (
		INSERT INTO no_show_orders (
			id,
			consumer_id,
			store_id,
			status,
			total_price,
			visit_time,
			store_memo,
			menu_names,
			order_no,
			payment_method,
			payment_status,
			paid_amount,
			payment_tx_id,
			payment_memo,
			paid_at,
			created_at,
			updated_at
		)
		SELECT
			gen_random_uuid(),
			consumer_id,
			main_store_id,
			CASE WHEN rn % 9 = 0 THEN 'CANCELLED'::order_status ELSE 'COMPLETED'::order_status END,
			first_original_price + second_original_price,
			visit_time,
			NULL,
			format('%s(1), %s(1)', first_menu_name, second_menu_name),
			order_no,
			'CARD',
			'PAID',
			first_discounted_price + second_discounted_price,
			format('perf-export-%s', lpad(rn::text, 6, '0')),
			'PERF export seed',
			created_at,
			created_at,
			created_at
		FROM order_source
		RETURNING id, order_no, created_at, visit_time
	)
	INSERT INTO perf_tmp_export_orders (rn, order_id, order_no, created_at, visit_time)
	SELECT
		os.rn,
		io.id,
		os.order_no,
		io.created_at,
		io.visit_time
	FROM order_source os
	JOIN inserted_orders io ON io.order_no = os.order_no;

	INSERT INTO no_show_order_items (
		order_id,
		post_id,
		menu_id,
		menu_name,
		quantity,
		unit_price,
		discount_percent,
		visit_time,
		created_at,
		updated_at
	)
	SELECT
		eo.order_id,
		os.first_post_id,
		os.first_menu_id,
		os.first_menu_name,
		1,
		os.first_discounted_price,
		os.discount_percent,
		eo.visit_time,
		eo.created_at,
		eo.created_at
	FROM perf_tmp_export_orders eo
	JOIN (
		SELECT
			gs AS rn,
			p1.post_id AS first_post_id,
			p1.menu_id AS first_menu_id,
			p1.menu_name AS first_menu_name,
			p1.discounted_price AS first_discounted_price,
			p1.discount_percent AS discount_percent
		FROM generate_series(1, export_order_count) gs
		JOIN perf_tmp_main_posts p1 ON p1.rn = ((gs - 1) % GREATEST(main_open_post_count - 1, 1)) + 2
	) os ON os.rn = eo.rn
	UNION ALL
	SELECT
		eo.order_id,
		os.second_post_id,
		os.second_menu_id,
		os.second_menu_name,
		1,
		os.second_discounted_price,
		os.discount_percent,
		eo.visit_time,
		eo.created_at,
		eo.created_at
	FROM perf_tmp_export_orders eo
	JOIN (
		SELECT
			gs AS rn,
			p2.post_id AS second_post_id,
			p2.menu_id AS second_menu_id,
			p2.menu_name AS second_menu_name,
			p2.discounted_price AS second_discounted_price,
			p2.discount_percent AS discount_percent
		FROM generate_series(1, export_order_count) gs
		JOIN perf_tmp_main_posts p2 ON p2.rn = (gs % GREATEST(main_open_post_count - 1, 1)) + 2
	) os ON os.rn = eo.rn;

	WITH todays_home_orders AS (
		SELECT
			gs AS rn,
			p.post_id,
			p.menu_id,
			p.menu_name,
			p.original_price,
			p.discounted_price,
			p.discount_percent,
			seed_now - make_interval(mins => gs * 10) AS created_at,
			seed_now + interval '90 minutes' AS visit_time,
			format('NS-%s-H%s', to_char(seed_now, 'YYYYMMDD'), lpad(gs::text, 4, '0')) AS order_no
		FROM generate_series(1, 3) gs
		JOIN perf_tmp_main_posts p ON p.rn = gs + 1
	),
	inserted_home_orders AS (
		INSERT INTO no_show_orders (
			id,
			consumer_id,
			store_id,
			status,
			total_price,
			visit_time,
			store_memo,
			menu_names,
			order_no,
			payment_method,
			payment_status,
			paid_amount,
			payment_tx_id,
			payment_memo,
			paid_at,
			created_at,
			updated_at
		)
		SELECT
			gen_random_uuid(),
			consumer_id,
			main_store_id,
			CASE
				WHEN rn = 1 THEN 'PENDING'::order_status
				WHEN rn = 2 THEN 'COMPLETED'::order_status
				ELSE 'CONFIRMED'::order_status
			END,
			original_price,
			visit_time,
			NULL,
			format('%s(1)', menu_name),
			order_no,
			'CARD',
			'PAID',
			discounted_price,
			format('perf-home-%s', lpad(rn::text, 4, '0')),
			'PERF home seed',
			created_at,
			created_at,
			created_at
		FROM todays_home_orders
		RETURNING id, order_no, created_at, visit_time
	)
	INSERT INTO no_show_order_items (
		order_id,
		post_id,
		menu_id,
		menu_name,
		quantity,
		unit_price,
		discount_percent,
		visit_time,
		created_at,
		updated_at
	)
	SELECT
		iho.id,
		tho.post_id,
		tho.menu_id,
		tho.menu_name,
		1,
		tho.discounted_price,
		tho.discount_percent,
		iho.visit_time,
		iho.created_at,
		iho.created_at
	FROM inserted_home_orders iho
	JOIN todays_home_orders tho ON tho.order_no = iho.order_no;
END;
$$;
