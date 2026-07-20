-- Split no-show preset/schedule timing fields into visit and sale delay.

ALTER TABLE no_show_presets
    ADD COLUMN IF NOT EXISTS visit_available_minutes INTEGER,
    ADD COLUMN IF NOT EXISTS sale_delay_minutes INTEGER;

UPDATE no_show_presets
SET visit_available_minutes = COALESCE(visit_available_minutes, delay_minutes),
    sale_delay_minutes = COALESCE(sale_delay_minutes, delay_minutes);

ALTER TABLE no_show_presets
    ALTER COLUMN visit_available_minutes SET NOT NULL,
    ALTER COLUMN sale_delay_minutes SET NOT NULL;

ALTER TABLE no_show_presets
    ADD CONSTRAINT chk_no_show_presets_visit_available_minutes
        CHECK (visit_available_minutes BETWEEN 1 AND 300),
    ADD CONSTRAINT chk_no_show_presets_sale_delay_minutes
        CHECK (sale_delay_minutes BETWEEN 0 AND 300);

ALTER TABLE no_show_post_schedules
    ADD COLUMN IF NOT EXISTS visit_available_minutes INTEGER,
    ADD COLUMN IF NOT EXISTS sale_delay_minutes INTEGER;

UPDATE no_show_post_schedules
SET visit_available_minutes = COALESCE(visit_available_minutes, delay_minutes),
    sale_delay_minutes = COALESCE(sale_delay_minutes, delay_minutes);

ALTER TABLE no_show_post_schedules
    ALTER COLUMN visit_available_minutes SET NOT NULL,
    ALTER COLUMN sale_delay_minutes SET NOT NULL;

ALTER TABLE no_show_post_schedules
    ADD CONSTRAINT chk_no_show_post_schedules_visit_available_minutes
        CHECK (visit_available_minutes BETWEEN 1 AND 300),
    ADD CONSTRAINT chk_no_show_post_schedules_sale_delay_minutes
        CHECK (sale_delay_minutes BETWEEN 0 AND 300);
