package com.app.feed;

/**
 * Feed 流类型
 */
public enum FeedType {

    /** 广场：所有用户的最新帖文（默认，SQL 直查） */
    PLAZA,

    /** 关注：当前用户关注的所有用户的最新帖文（推送模式） */
    FOLLOWING,

    /** 附近：按当前用户位置过滤的帖文（SQL 直查） */
    NEARBY;

    /**
     * 解析 Feed 类型字符串，未知值抛异常
     */
    public static FeedType from(String type) {
        if (type == null || type.isBlank()) {
            return PLAZA;
        }
        return switch (type.trim().toLowerCase()) {
            case "plaza", "square" -> PLAZA;
            case "following", "follow" -> FOLLOWING;
            case "nearby", "near" -> NEARBY;
            default -> throw new IllegalArgumentException("未知的 feed 类型: " + type
                    + "（可选值：plaza / following / nearby）");
        };
    }
}
