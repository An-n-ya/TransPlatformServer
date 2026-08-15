package com.app.search;

import com.app.common.PageResult;
import org.springframework.data.domain.Pageable;

/**
 * 搜索 Service 接口
 */
public interface SearchService {

    /**
     * 按类别搜索
     *
     * @param category 类别标识（如 "user"）
     * @param keyword  搜索关键词
     * @param pageable 分页参数
     * @return 搜索结果（包含类别、关键词、内容列表）
     */
    SearchResult<?> search(String category, String keyword, Pageable pageable);
}
