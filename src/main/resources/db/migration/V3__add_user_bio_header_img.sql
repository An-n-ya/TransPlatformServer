-- ============================================================
-- V3: 用户表增加 bio_header_img 字段
-- ============================================================

ALTER TABLE users
  ADD COLUMN bio_header_img VARCHAR(500) DEFAULT NULL COMMENT '个人主页背景图URL' AFTER bio;
