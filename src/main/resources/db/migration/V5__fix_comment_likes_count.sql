-- ============================================================
-- V5: 修复评论点赞计数（历史点赞记录未同步到 comments.likes_count）
-- ============================================================

UPDATE comments c
SET c.likes_count = (
    SELECT COUNT(*)
    FROM likes l
    WHERE l.target_type = 'comment' AND l.target_id = c.id
);
