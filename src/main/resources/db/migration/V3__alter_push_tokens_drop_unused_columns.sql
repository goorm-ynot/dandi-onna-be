-- Remove unused columns from push_tokens. We only rely on device_id + user context now.
ALTER TABLE push_tokens
	DROP COLUMN IF EXISTS webpush_endpoint,
	DROP COLUMN IF EXISTS is_revoked;
