-- Add unique constraint to prevent duplicate menu/time combinations
ALTER TABLE no_show_posts
    ADD CONSTRAINT uniq_no_show_post_store_menu_expire
        UNIQUE (store_id, menu_id, expire_at);

-- History table for overwritten or closed no-show posts
CREATE TABLE no_show_post_history (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    store_id UUID NOT NULL,
    menu_id UUID NOT NULL,
    price_percent INTEGER NOT NULL,
    price_krw INTEGER NOT NULL,
    original_krw INTEGER,
    qty_total INTEGER NOT NULL,
    qty_remaining INTEGER NOT NULL,
    start_at TIMESTAMPTZ NOT NULL,
    expire_at TIMESTAMPTZ NOT NULL,
    status no_show_post_status NOT NULL,
    replaced_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reason TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_no_show_post_history_post
        FOREIGN KEY (post_id) REFERENCES no_show_posts(id)
);

CREATE INDEX idx_no_show_post_history_post_id ON no_show_post_history(post_id);
CREATE INDEX idx_no_show_post_history_store_menu ON no_show_post_history(store_id, menu_id);
