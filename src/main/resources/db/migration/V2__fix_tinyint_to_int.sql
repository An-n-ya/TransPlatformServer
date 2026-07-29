-- ============================================================
-- V2: 将 TINYINT 字段改为 INT，与 JPA Integer 类型匹配
-- ============================================================

ALTER TABLE users      MODIFY COLUMN status     INT NOT NULL DEFAULT 1;
ALTER TABLE follows    MODIFY COLUMN status     INT NOT NULL DEFAULT 1;
ALTER TABLE posts      MODIFY COLUMN status     INT NOT NULL DEFAULT 1;
ALTER TABLE comments   MODIFY COLUMN status     INT NOT NULL DEFAULT 1;
ALTER TABLE messages   MODIFY COLUMN status     INT NOT NULL DEFAULT 1;
ALTER TABLE notifications MODIFY COLUMN is_read INT NOT NULL DEFAULT 0;
