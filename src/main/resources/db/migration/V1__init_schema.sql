-- ============================================================
-- TransPlatform - SQLite 初始化 Schema (V1)
-- ============================================================

-- 用户表
CREATE TABLE users (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    nickname        VARCHAR(100) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    avatar          VARCHAR(500),
    bio             VARCHAR(200),
    bio_header_img  VARCHAR(500),
    pinned_post_id  INTEGER,
    status          INTEGER      NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 关注关系表
CREATE TABLE follows (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    follower_id INTEGER  NOT NULL,
    followee_id INTEGER  NOT NULL,
    status      INTEGER  NOT NULL DEFAULT 1,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (follower_id, followee_id)
);
CREATE INDEX idx_follower ON follows (follower_id);
CREATE INDEX idx_followee ON follows (followee_id);

-- 帖文表
CREATE TABLE posts (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id           INTEGER      NOT NULL,
    content           TEXT,
    images            TEXT,
    location          VARCHAR(200),
    likes_count       INTEGER      NOT NULL DEFAULT 0,
    comments_count    INTEGER      NOT NULL DEFAULT 0,
    collections_count INTEGER      NOT NULL DEFAULT 0,
    status            INTEGER      NOT NULL DEFAULT 1,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_posts_user ON posts (user_id);
CREATE INDEX idx_posts_created ON posts (created_at);
CREATE INDEX idx_posts_status_created ON posts (status, created_at);

-- 帖文图片表
CREATE TABLE post_images (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id    INTEGER      NOT NULL,
    url        VARCHAR(500) NOT NULL,
    width      INTEGER,
    height     INTEGER,
    size       INTEGER,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_post_images_post ON post_images (post_id);

-- 点赞表
CREATE TABLE likes (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id   INTEGER NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, target_type, target_id)
);
CREATE INDEX idx_likes_target ON likes (target_type, target_id);
CREATE INDEX idx_likes_user ON likes (user_id);

-- 评论表
CREATE TABLE comments (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id          INTEGER       NOT NULL,
    user_id          INTEGER       NOT NULL,
    parent_id        INTEGER,
    reply_to_user_id INTEGER,
    content          VARCHAR(1000) NOT NULL,
    likes_count      INTEGER       NOT NULL DEFAULT 0,
    status           INTEGER       NOT NULL DEFAULT 1,
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_comments_post ON comments (post_id);
CREATE INDEX idx_comments_user ON comments (user_id);
CREATE INDEX idx_comments_parent ON comments (parent_id);

-- 收藏表
CREATE TABLE collections (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL,
    post_id    INTEGER NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, post_id)
);
CREATE INDEX idx_collections_user ON collections (user_id);
CREATE INDEX idx_collections_post ON collections (post_id);

-- 私信会话表
CREATE TABLE sessions (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    type       VARCHAR(20) NOT NULL DEFAULT 'single',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 会话参与者表
CREATE TABLE session_participants (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id   INTEGER NOT NULL,
    user_id      INTEGER NOT NULL,
    last_read_at DATETIME,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (session_id, user_id)
);
CREATE INDEX idx_participants_user ON session_participants (user_id);

-- 私信消息表
CREATE TABLE messages (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id INTEGER NOT NULL,
    sender_id  INTEGER NOT NULL,
    type       VARCHAR(20) NOT NULL DEFAULT 'text',
    content    TEXT NOT NULL,
    status     INTEGER NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_messages_session ON messages (session_id);
CREATE INDEX idx_messages_created ON messages (created_at);

-- 通知表
CREATE TABLE notifications (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER      NOT NULL,
    type         VARCHAR(30)  NOT NULL,
    title        VARCHAR(200),
    content      VARCHAR(500),
    from_user_id INTEGER,
    target_id    INTEGER,
    is_read      INTEGER      NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_notifications_user ON notifications (user_id);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id, is_read);
CREATE INDEX idx_notifications_created ON notifications (created_at);
