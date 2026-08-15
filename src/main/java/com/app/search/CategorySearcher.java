package com.app.search;

import com.app.common.PageResult;
import org.springframework.data.domain.Pageable;

/**
 * 搜索类别策略接口 — 每个搜索类别实现一个，注册到 SearchServiceImpl
 *
 * 新增类别（如 topic）只需：
 *   1. 实现本接口
 *   2. 标注 @Component 并返回对应 category() 标识
 *   3. SearchServiceImpl 会自动收集注册
 */
public interface CategorySearcher {

    /**
     * 类别标识，例如 "user"、"topic"
     */
    String category();

    /**
     * 执行搜索
     * @param keyword  搜索关键词
     * @param pageable 分页参数
     * @return 该类别下的搜索结果
     */
    PageResult<?> search(String keyword, Pageable pageable);
}
