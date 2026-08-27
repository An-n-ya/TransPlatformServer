-- ============================================================
-- V7: 用户位置
--   - users 表新增 location 字段（城市，用于“附近”时间流过滤）
-- ============================================================

ALTER TABLE users ADD COLUMN location VARCHAR(200);
CREATE INDEX idx_users_location ON users (location);
