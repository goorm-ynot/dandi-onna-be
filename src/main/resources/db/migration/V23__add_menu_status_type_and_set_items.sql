CREATE TYPE menu_status AS ENUM ('on_sale', 'sold_out');
CREATE TYPE menu_type AS ENUM ('single', 'set');

ALTER TABLE menus
    ADD COLUMN status menu_status,
    ADD COLUMN type menu_type;

UPDATE menus
SET status = 'on_sale',
    type = 'single'
WHERE status IS NULL
   OR type IS NULL;

ALTER TABLE menus
    ALTER COLUMN status SET DEFAULT 'sold_out',
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN type SET DEFAULT 'single',
    ALTER COLUMN type SET NOT NULL;

CREATE TABLE menu_set_items (
    id BIGSERIAL PRIMARY KEY,
    set_menu_id UUID NOT NULL REFERENCES menus(id) ON DELETE CASCADE,
    component_menu_id UUID NOT NULL REFERENCES menus(id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL,
    CONSTRAINT uniq_menu_set_items UNIQUE (set_menu_id, component_menu_id),
    CONSTRAINT chk_menu_set_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_menu_set_items_self_reference CHECK (set_menu_id <> component_menu_id)
);

CREATE INDEX idx_menu_set_items_set_menu_id ON menu_set_items(set_menu_id);
CREATE INDEX idx_menu_set_items_component_menu_id ON menu_set_items(component_menu_id);
