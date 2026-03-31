ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_token VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_token_expiry TIMESTAMP(6);
CREATE INDEX IF NOT EXISTS idx_users_password_reset_token ON users(password_reset_token);
