-- Reset seeded CEO data and change phone columns back to text so hyphenated numbers are preserved
DELETE FROM store_hours;
DELETE FROM stores;
DELETE FROM owner_profiles;
DELETE FROM users WHERE login_id LIKE 'CEO%';

ALTER TABLE owner_profiles
	ALTER COLUMN phone TYPE TEXT;

ALTER TABLE stores
	ALTER COLUMN phone TYPE TEXT;
