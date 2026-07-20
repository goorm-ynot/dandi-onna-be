-- Remove expires_at column from push_tokens as we now manage validity via last_seen_at only.
ALTER TABLE push_tokens
	DROP COLUMN IF EXISTS expires_at;