ALTER TABLE stores
	ADD COLUMN image_key TEXT,
	ADD COLUMN image_mime TEXT,
	ADD COLUMN image_etag TEXT,
	ADD COLUMN image_status menu_image_status NOT NULL DEFAULT 'pending';
