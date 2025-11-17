ALTER TABLE menus
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS image_key TEXT,
    ADD COLUMN IF NOT EXISTS image_mime TEXT,
    ADD COLUMN IF NOT EXISTS image_etag TEXT,
    ADD COLUMN IF NOT EXISTS image_status menu_image_status NOT NULL DEFAULT 'pending';
