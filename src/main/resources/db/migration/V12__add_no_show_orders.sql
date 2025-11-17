CREATE TABLE no_show_orders (
    id BIGSERIAL PRIMARY KEY,
    consumer_id UUID NOT NULL REFERENCES users(id),
    store_id UUID NOT NULL REFERENCES stores(id),
    status order_status NOT NULL DEFAULT 'PENDING',
    total_price INTEGER NOT NULL CHECK (total_price >= 0),
    visit_time TIMESTAMPTZ NOT NULL,
    store_memo TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_no_show_orders_consumer ON no_show_orders(consumer_id, created_at DESC);
CREATE INDEX idx_no_show_orders_store ON no_show_orders(store_id, created_at DESC);
CREATE INDEX idx_no_show_orders_status_pending ON no_show_orders(status) WHERE status = 'PENDING';

CREATE TABLE no_show_order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES no_show_orders(id) ON DELETE CASCADE,
    post_id BIGINT NOT NULL REFERENCES no_show_posts(id),
    menu_id UUID NOT NULL REFERENCES menus(id),
    quantity SMALLINT NOT NULL CHECK (quantity > 0),
    unit_price INTEGER NOT NULL CHECK (unit_price >= 0),
    discount_percent INTEGER NOT NULL CHECK (discount_percent BETWEEN 0 AND 100),
    visit_time TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_no_show_order_items_order ON no_show_order_items(order_id);
CREATE INDEX idx_no_show_order_items_post ON no_show_order_items(post_id);
