-- ============================================================
-- V4: 用户表增加 pinned_post_id 字段（置顶帖）
-- ============================================================

ALTER TABLE users
  ADD COLUMN pinned_post_id BIGINT DEFAULT NULL COMMENT '置顶帖文ID' AFTER bio_header_img;
