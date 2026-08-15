-- ============================================================
-- V2: 修复评论点赞计数（历史点赞记录未同步到 comments.likes_count）
-- ============================================================

UPDATE comments SET likes_count = (
    SELECT COUNT(*)
    FROM likes
    WHERE likes.target_type = 'comment' AND likes.target_id = comments.id
);
