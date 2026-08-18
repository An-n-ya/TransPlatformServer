-- ============================================================
-- V5: 用户邮箱与验证码
--   - users 表新增 email 字段（用于验证邮箱 / 找回密码）
-- ============================================================

-- 用户邮箱（可空，绑定后唯一；未绑定时允许多个 NULL）
ALTER TABLE users ADD COLUMN email VARCHAR(255);
CREATE UNIQUE INDEX idx_users_email ON users (email);
