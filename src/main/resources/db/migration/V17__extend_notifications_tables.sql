-- Extend notifications / notification_targets to support logging, retries, and monitoring.

ALTER TABLE notifications
  ADD COLUMN IF NOT EXISTS category TEXT,
  ADD COLUMN IF NOT EXISTS priority TEXT DEFAULT 'NORMAL',
  ADD COLUMN IF NOT EXISTS retries INT DEFAULT 0,
  ADD COLUMN IF NOT EXISTS max_retries INT DEFAULT 3;

ALTER TABLE notification_targets
  ADD COLUMN IF NOT EXISTS attempt_count INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS last_error_code TEXT,
  ADD COLUMN IF NOT EXISTS last_error_message TEXT,
  ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS channel TEXT DEFAULT 'FCM',
  ADD COLUMN IF NOT EXISTS message_id TEXT,
  ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_notification_targets_status_retry
  ON notification_targets(status, next_retry_at);

CREATE INDEX IF NOT EXISTS idx_notification_targets_user_status
  ON notification_targets(user_id, status);
