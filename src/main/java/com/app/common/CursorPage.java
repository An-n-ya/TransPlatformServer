package com.app.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 游标分页结果 — 用于动态列表（如 Feed），避免 offset 分页在列表变化时重复/漏帖。
 *
 * @param <T> 列表元素类型
 */
@Data
@AllArgsConstructor
public class CursorPage<T> {

    /** 当前页数据 */
    private List<T> content;

    /**
     * 下一页游标 = 本页最后一条可见数据的 id（opaque token，原样传回即可）。
     * content 非空时始终返回；为 null 表示没有更多数据。
     */
    private Long nextCursor;

    /** 是否还有更多 */
    private boolean hasMore;

    public static <T> CursorPage<T> empty() {
        return new CursorPage<>(List.of(), null, false);
    }
}
