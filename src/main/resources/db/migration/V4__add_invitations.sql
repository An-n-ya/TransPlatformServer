-- ============================================================
-- V4: 邀请注册机制
--   - invitations 表
--   - users 表新增 role 字段
-- ============================================================

-- 邀请码表
CREATE TABLE invitations (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    code       VARCHAR(16) NOT NULL UNIQUE,
    inviter_id INTEGER     NOT NULL,
    invitee_id INTEGER,
    status     INTEGER     NOT NULL DEFAULT 0,
    expired_at DATETIME    NOT NULL,
    scene      VARCHAR(30) NOT NULL DEFAULT 'default',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_invitations_inviter ON invitations (inviter_id);
CREATE UNIQUE INDEX idx_invitations_invitee ON invitations (invitee_id);
CREATE INDEX idx_invitations_status_expired ON invitations (status, expired_at);

-- users 表新增角色字段
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'user';
