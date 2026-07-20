-- Remove legacy single-delay columns after split into visit_available_minutes/sale_delay_minutes.

ALTER TABLE no_show_presets
    DROP COLUMN IF EXISTS delay_minutes;

ALTER TABLE no_show_post_schedules
    DROP COLUMN IF EXISTS delay_minutes;
