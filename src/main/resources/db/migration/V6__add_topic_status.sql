-- ============================================================
-- V6: 话题逻辑删除
--   - topics 表新增 status 字段（1=正常 0=已删除）
--   管理后台删除话题时仅置 status=0，不再物理删除
-- ============================================================

ALTER TABLE topics ADD COLUMN status INTEGER NOT NULL DEFAULT 1;
CREATE INDEX idx_topics_status ON topics (status);
