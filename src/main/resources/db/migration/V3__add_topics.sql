-- ============================================================
-- V3: 话题表 + 帖文话题关联表
-- ============================================================

-- 话题表
CREATE TABLE topics (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(200),
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_topics_name ON topics (name);

-- 帖文-话题关联表（多对多）
CREATE TABLE post_topics (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id    INTEGER NOT NULL,
    topic_id   INTEGER NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (post_id, topic_id)
);
CREATE INDEX idx_post_topics_post ON post_topics (post_id);
CREATE INDEX idx_post_topics_topic ON post_topics (topic_id);
