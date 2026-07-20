-- Rename price columns to clarify unit prices
ALTER TABLE no_show_posts
    RENAME COLUMN price_krw TO discounted_unit_price;

ALTER TABLE no_show_posts
    RENAME COLUMN original_krw TO original_unit_price;

ALTER TABLE no_show_post_history
    RENAME COLUMN price_krw TO discounted_unit_price;

ALTER TABLE no_show_post_history
    RENAME COLUMN original_krw TO original_unit_price;
