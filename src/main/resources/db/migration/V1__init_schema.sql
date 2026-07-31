-- ============================================================
-- TransPlatform - 初始化数据库 Schema
-- ============================================================

-- 用户表
CREATE TABLE users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    nickname    VARCHAR(100) NOT NULL,
    password    VARCHAR(255) NOT NULL COMMENT 'BCrypt 加密后的密码',
    avatar      VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    bio              VARCHAR(200)  DEFAULT NULL COMMENT '个人简介',
    bio_header_img   VARCHAR(500)  DEFAULT NULL COMMENT '个人主页背景图URL',
    pinned_post_id   BIGINT        DEFAULT NULL COMMENT '置顶帖文ID',
    status           INT           NOT NULL DEFAULT 1 COMMENT '1:正常 0:禁用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 关注关系表
CREATE TABLE follows (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_id BIGINT   NOT NULL COMMENT '关注者ID',
    followee_id BIGINT   NOT NULL COMMENT '被关注者ID',
    status      INT      NOT NULL DEFAULT 1 COMMENT '1:有效 0:取消',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_follower_followee (follower_id, followee_id),
    INDEX idx_follower (follower_id),
    INDEX idx_followee (followee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注关系表';

-- 帖文表
CREATE TABLE posts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL COMMENT '发布者ID',
    content         TEXT         DEFAULT NULL COMMENT '文字内容',
    images          JSON         DEFAULT NULL COMMENT '图片URL数组 ["url1","url2"]',
    location        VARCHAR(200) DEFAULT NULL COMMENT '发布位置',
    likes_count     INT          NOT NULL DEFAULT 0 COMMENT '点赞数',
    comments_count  INT          NOT NULL DEFAULT 0 COMMENT '评论数',
    collections_count INT        NOT NULL DEFAULT 0 COMMENT '收藏数',
    status          INT          NOT NULL DEFAULT 1 COMMENT '1:正常 0:删除 2:审核中',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖文表';

-- 帖文图片表（独立存储图片元信息）
CREATE TABLE post_images (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id     BIGINT       NOT NULL,
    url         VARCHAR(500) NOT NULL COMMENT '图片访问URL',
    width       INT          DEFAULT NULL COMMENT '图片宽度',
    height      INT          DEFAULT NULL COMMENT '图片高度',
    size        BIGINT       DEFAULT NULL COMMENT '文件大小(字节)',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序序号',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_post_id (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖文图片表';

-- 点赞表
CREATE TABLE likes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    target_type VARCHAR(20) NOT NULL COMMENT '点赞目标类型: post/comment',
    target_id   BIGINT      NOT NULL COMMENT '目标ID（帖文ID或评论ID）',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_target (user_id, target_type, target_id),
    INDEX idx_target (target_type, target_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞表';

-- 评论表
CREATE TABLE comments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id         BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    parent_id       BIGINT       DEFAULT NULL COMMENT '父评论ID，支持嵌套回复',
    reply_to_user_id BIGINT      DEFAULT NULL COMMENT '回复的目标用户ID',
    content         VARCHAR(1000) NOT NULL,
    likes_count     INT          NOT NULL DEFAULT 0,
    status          INT          NOT NULL DEFAULT 1 COMMENT '1:正常 0:删除',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- 收藏表
CREATE TABLE collections (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL,
    post_id     BIGINT   NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_post (user_id, post_id),
    INDEX idx_user_id (user_id),
    INDEX idx_post_id (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- 私信会话表
CREATE TABLE sessions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    type        VARCHAR(20) NOT NULL DEFAULT 'single' COMMENT '会话类型: single/group',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信会话表';

-- 会话参与者表
CREATE TABLE session_participants (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id  BIGINT   NOT NULL,
    user_id     BIGINT   NOT NULL,
    last_read_at DATETIME DEFAULT NULL COMMENT '最后阅读时间',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_session_user (session_id, user_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话参与者表';

-- 私信消息表
CREATE TABLE messages (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id  BIGINT       NOT NULL,
    sender_id   BIGINT       NOT NULL,
    type        VARCHAR(20)  NOT NULL DEFAULT 'text' COMMENT '消息类型: text/image',
    content     TEXT         NOT NULL,
    status      INT          NOT NULL DEFAULT 1 COMMENT '1:正常 0:撤回',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信消息表';

-- 通知表
CREATE TABLE notifications (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL COMMENT '接收通知的用户ID',
    type        VARCHAR(30)  NOT NULL COMMENT '通知类型: like/comment/follow/system',
    title       VARCHAR(200) DEFAULT NULL,
    content     VARCHAR(500) DEFAULT NULL,
    from_user_id BIGINT      DEFAULT NULL COMMENT '触发通知的用户ID',
    target_id   BIGINT       DEFAULT NULL COMMENT '关联目标ID',
    is_read     INT          NOT NULL DEFAULT 0 COMMENT '0:未读 1:已读',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_user_unread (user_id, is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';
