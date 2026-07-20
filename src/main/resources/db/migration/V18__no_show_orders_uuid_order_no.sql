-- Migrate no_show_orders PK to UUID and add order_no (display-friendly).

-- 1) Add UUID id column for orders + backfill.
ALTER TABLE no_show_orders
	ADD COLUMN id_uuid UUID;

UPDATE no_show_orders
SET id_uuid = gen_random_uuid()
WHERE id_uuid IS NULL;

-- 2) Add order_no and backfill with deterministic value.
ALTER TABLE no_show_orders
	ADD COLUMN order_no TEXT;

UPDATE no_show_orders
SET order_no = 'NS-' || to_char(created_at, 'YYYYMMDD') || '-' || upper(substr(md5(id_uuid::text), 1, 6))
WHERE order_no IS NULL;

ALTER TABLE no_show_orders
	ALTER COLUMN order_no SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_no_show_orders_order_no ON no_show_orders(order_no);

-- 3) Add UUID FK in items and backfill.
ALTER TABLE no_show_order_items
	ADD COLUMN order_id_uuid UUID;

UPDATE no_show_order_items i
SET order_id_uuid = o.id_uuid
FROM no_show_orders o
WHERE i.order_id = o.id;

-- 4) Drop old constraints and swap PK/FK.
ALTER TABLE no_show_order_items
	DROP CONSTRAINT IF EXISTS no_show_order_items_order_id_fkey;

ALTER TABLE no_show_orders
	DROP CONSTRAINT IF EXISTS no_show_orders_pkey;

ALTER TABLE no_show_orders
	RENAME COLUMN id TO legacy_id;

ALTER TABLE no_show_orders
	RENAME COLUMN id_uuid TO id;

ALTER TABLE no_show_order_items
	RENAME COLUMN order_id TO legacy_order_id;

ALTER TABLE no_show_order_items
	RENAME COLUMN order_id_uuid TO order_id;

ALTER TABLE no_show_orders
	ALTER COLUMN id SET NOT NULL;

ALTER TABLE no_show_orders
	ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE no_show_order_items
	ALTER COLUMN order_id SET NOT NULL;

ALTER TABLE no_show_orders
	ADD CONSTRAINT no_show_orders_pkey PRIMARY KEY (id);

ALTER TABLE no_show_order_items
	ADD CONSTRAINT no_show_order_items_order_id_fkey
	FOREIGN KEY (order_id) REFERENCES no_show_orders(id) ON DELETE CASCADE;

DROP INDEX IF EXISTS idx_no_show_order_items_order;
CREATE INDEX idx_no_show_order_items_order ON no_show_order_items(order_id);

-- 5) Drop legacy columns (data already migrated).
ALTER TABLE no_show_orders
	DROP COLUMN legacy_id;

ALTER TABLE no_show_order_items
	DROP COLUMN legacy_order_id;
