-- Preset + delayed no-show posting schedules

DO $$
BEGIN
    CREATE TYPE no_show_schedule_status AS ENUM ('QUEUED', 'PROCESSING', 'PUBLISHED', 'CANCELLED', 'FAILED');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

CREATE TABLE no_show_presets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id),
    name VARCHAR(50) NOT NULL,
    discount_percent INTEGER NOT NULL CHECK (discount_percent BETWEEN 30 AND 90),
    delay_minutes INTEGER NOT NULL CHECK (delay_minutes BETWEEN 1 AND 300),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX uniq_no_show_presets_store_default_active
    ON no_show_presets(store_id)
    WHERE is_default AND active AND deleted_at IS NULL;

CREATE INDEX idx_no_show_presets_store_id ON no_show_presets(store_id);

CREATE TABLE no_show_post_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id),
    requested_by UUID NOT NULL REFERENCES users(id),
    preset_id UUID REFERENCES no_show_presets(id),
    status no_show_schedule_status NOT NULL DEFAULT 'QUEUED',
    discount_percent INTEGER NOT NULL CHECK (discount_percent BETWEEN 30 AND 90),
    delay_minutes INTEGER NOT NULL CHECK (delay_minutes BETWEEN 1 AND 300),
    start_at TIMESTAMPTZ NOT NULL,
    expire_at TIMESTAMPTZ NOT NULL,
    published_post_count INTEGER,
    published_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    error_message TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT no_show_post_schedules_time_range CHECK (start_at < expire_at)
);

CREATE INDEX idx_no_show_post_schedules_store_id ON no_show_post_schedules(store_id);
CREATE INDEX idx_no_show_post_schedules_requested_by ON no_show_post_schedules(requested_by);
CREATE INDEX idx_no_show_post_schedules_status_start_at ON no_show_post_schedules(status, start_at);
CREATE INDEX idx_no_show_post_schedules_due_queue
    ON no_show_post_schedules(start_at)
    WHERE active AND status = 'QUEUED';

CREATE TABLE no_show_post_schedule_items (
    id BIGSERIAL PRIMARY KEY,
    schedule_id UUID NOT NULL REFERENCES no_show_post_schedules(id) ON DELETE CASCADE,
    menu_id UUID NOT NULL REFERENCES menus(id),
    quantity SMALLINT NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uniq_no_show_post_schedule_items UNIQUE (schedule_id, menu_id)
);

CREATE INDEX idx_no_show_post_schedule_items_schedule_id ON no_show_post_schedule_items(schedule_id);
CREATE INDEX idx_no_show_post_schedule_items_menu_id ON no_show_post_schedule_items(menu_id);
