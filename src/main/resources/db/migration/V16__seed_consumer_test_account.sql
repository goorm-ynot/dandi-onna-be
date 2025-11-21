-- Seed a default consumer account for manual testing (Customer1 / 111111).
-- Re-runnable: updates existing rows to keep data in sync.

WITH upsert_user AS (
  INSERT INTO users (login_id, password_hash, role)
  VALUES (
    'Customer1',
    '$2y$10$HpMsB1IR/9CQuxPrS/l5B.bmDJz2q1onSKui5BCu0gtVEqxOrpRO2',
    'CONSUMER'
  )
  ON CONFLICT (login_id) DO UPDATE
  SET password_hash = EXCLUDED.password_hash,
      role = EXCLUDED.role,
      updated_at = now()
  RETURNING id
)
INSERT INTO consumer_profiles (user_id, name, phone, terms_agreed, terms_version)
SELECT
  id,
  'Customer1',
  '010-1111-2222',
  TRUE,
  'v1'
FROM upsert_user
ON CONFLICT (user_id) DO UPDATE
SET name = EXCLUDED.name,
    phone = EXCLUDED.phone,
    terms_agreed = EXCLUDED.terms_agreed,
    terms_version = EXCLUDED.terms_version,
    updated_at = now();
