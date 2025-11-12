-- Base schema for notifications / orders / stores domain.
-- Flyway migration: creates types, tables, and indexes described in the spec.

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS postgis;

-- Enumerations
CREATE TYPE platform AS ENUM ('WEB', 'ANDROID');
CREATE TYPE user_role AS ENUM ('CONSUMER', 'OWNER', 'ADMIN');
CREATE TYPE order_status AS ENUM ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED');
CREATE TYPE menu_image_status AS ENUM ('pending', 'uploaded', 'active');
CREATE TYPE no_show_post_status AS ENUM ('draft', 'open', 'sold_out', 'expired', 'canceled', 'closed');

-- Core users table
CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       login_id TEXT NOT NULL UNIQUE,
                       password_hash TEXT NOT NULL,
                       role user_role NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       deleted_at TIMESTAMPTZ
);

-- Push notification tokens
CREATE TABLE push_tokens (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             user_id UUID NOT NULL REFERENCES users(id),
                             platform platform NOT NULL,
                             device_id TEXT NOT NULL,
                             fcm_token TEXT NOT NULL,
                             webpush_endpoint TEXT,
                             user_agent TEXT,
                             last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                             expires_at TIMESTAMPTZ,
                             is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
                             CONSTRAINT push_tokens_user_platform_device_unique UNIQUE (user_id, platform, device_id)
);

CREATE INDEX idx_push_tokens_user_id ON push_tokens(user_id);

-- Notifications master
CREATE TABLE notifications (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               title TEXT,
                               body TEXT,
                               data JSONB,
                               created_by UUID REFERENCES users(id),
                               created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE notification_targets (
                                      id BIGSERIAL PRIMARY KEY,
                                      notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
                                      user_id UUID NOT NULL REFERENCES users(id),
                                      push_token_id UUID REFERENCES push_tokens(id) ON DELETE SET NULL,
                                      status TEXT NOT NULL DEFAULT 'QUEUED',
                                      error_code TEXT,
                                      sent_at TIMESTAMPTZ,
                                      delivered_at TIMESTAMPTZ
);

CREATE INDEX idx_notification_targets_notification_id ON notification_targets(notification_id);
CREATE INDEX idx_notification_targets_user_id ON notification_targets(user_id);

-- Owner domain
CREATE TABLE owner_profiles (
                                user_id UUID PRIMARY KEY REFERENCES users(id),
                                name VARCHAR(30) NOT NULL,
                                phone INT NOT NULL,
                                status BOOLEAN NOT NULL DEFAULT FALSE,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                deleted_at TIMESTAMPTZ
);

CREATE TABLE stores (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        owner_user_id UUID NOT NULL REFERENCES users(id),
                        name TEXT NOT NULL,
                        category TEXT NOT NULL DEFAULT '요식업',
                        phone INT NOT NULL,
                        address_road TEXT NOT NULL,
                        lat NUMERIC(9, 6) NOT NULL,
                        lon NUMERIC(9, 6) NOT NULL,
                        geom geometry(Point, 4326) GENERATED ALWAYS AS (ST_SetSRID(ST_MakePoint(lon, lat), 4326)) STORED,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                        deleted_at TIMESTAMPTZ,
                        CONSTRAINT stores_lat_check CHECK (lat BETWEEN -90 AND 90),
                        CONSTRAINT stores_lon_check CHECK (lon BETWEEN -180 AND 180)
);

CREATE INDEX idx_stores_owner_user_id ON stores(owner_user_id);
CREATE INDEX idx_stores_geom ON stores USING GIST (geom);

CREATE TABLE store_hours (
                             id BIGSERIAL PRIMARY KEY,
                             store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
                             dow SMALLINT NOT NULL DEFAULT 0,
                             seq SMALLINT NOT NULL DEFAULT 1,
                             open_time TIME NOT NULL DEFAULT TIME '10:00',
                             close_time TIME NOT NULL DEFAULT TIME '20:00',
                             created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                             updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                             deleted_at TIMESTAMPTZ,
                             CONSTRAINT store_hours_dow_range CHECK (dow BETWEEN 0 AND 6),
                             CONSTRAINT store_hours_open_before_close CHECK (open_time < close_time)
);

CREATE UNIQUE INDEX uniq_store_hours_store_dow_seq ON store_hours(store_id, dow, seq);

CREATE TABLE menus (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
                       name TEXT NOT NULL,
                       description TEXT,
                       price_krw INTEGER NOT NULL,
                       image_key TEXT,
                       image_mime TEXT,
                       image_etag TEXT,
                       image_status menu_image_status NOT NULL DEFAULT 'pending',
                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_menus_store_id ON menus(store_id);

CREATE TABLE no_show_posts (
                               id BIGSERIAL PRIMARY KEY,
                               store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
                               menu_id UUID NOT NULL REFERENCES menus(id),
                               price_percent INTEGER NOT NULL,
                               price_krw INTEGER NOT NULL,
                               original_krw INTEGER,
                               qty_total SMALLINT NOT NULL,
                               qty_remaining SMALLINT NOT NULL,
                               start_at TIMESTAMPTZ NOT NULL,
                               expire_at TIMESTAMPTZ NOT NULL,
                               status no_show_post_status NOT NULL DEFAULT 'draft',
                               created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                               updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                               deleted_at TIMESTAMPTZ,
                               CONSTRAINT qty_remaining_bounds CHECK (qty_remaining BETWEEN 0 AND qty_total)
);

CREATE INDEX idx_no_show_posts_store_id ON no_show_posts(store_id);
CREATE INDEX idx_no_show_posts_menu_id ON no_show_posts(menu_id);

-- Consumer domain
CREATE TABLE consumer_profiles (
                                   user_id UUID PRIMARY KEY REFERENCES users(id),
                                   name TEXT NOT NULL,
                                   phone TEXT NOT NULL,
                                   terms_agreed BOOLEAN NOT NULL DEFAULT TRUE,
                                   terms_version TEXT,
                                   created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                   updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                   deleted_at TIMESTAMPTZ
);

CREATE TABLE favorites (
                           consumer_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
                           created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                           PRIMARY KEY (consumer_user_id, store_id)
);

-- Orders
CREATE TABLE orders (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        consumer_user_id UUID NOT NULL REFERENCES users(id),
                        store_id UUID NOT NULL REFERENCES stores(id),
                        status order_status NOT NULL DEFAULT 'PENDING',
                        total_price_krw INTEGER NOT NULL,
                        paid_at TIMESTAMPTZ,
                        confirmed_at TIMESTAMPTZ,
                        cancelled_at TIMESTAMPTZ,
                        fulfilled_at TIMESTAMPTZ,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                        deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_orders_consumer_user_id ON orders(consumer_user_id);
CREATE INDEX idx_orders_store_id ON orders(store_id);
CREATE INDEX idx_orders_status ON orders(status);

CREATE TABLE order_items (
                             id BIGSERIAL PRIMARY KEY,
                             order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                             menu_id UUID NOT NULL REFERENCES menus(id),
                             quantity INTEGER NOT NULL CHECK (quantity > 0),
                             unit_price_krw INTEGER NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_menu_id ON order_items(menu_id);
