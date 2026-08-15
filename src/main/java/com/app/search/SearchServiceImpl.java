package com.app.search;

import com.app.common.PageResult;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 搜索实现 — 按类别分发到对应的策略实现
 *
 * Spring 会自动注入所有 CategorySearcher 实现（通过构造器注入 List），
 * 新增类别只需新增一个 @Component 实现类，无需改动本类。
 */
@Service
public class SearchServiceImpl implements SearchService {

    private final List<CategorySearcher> searchers;

    /** category → searcher 映射，构造时构建一次 */
    private final Map<String, CategorySearcher> searcherMap;

    public SearchServiceImpl(List<CategorySearcher> searchers) {
        this.searchers = searchers;
        this.searcherMap = searchers.stream()
                .collect(Collectors.toMap(CategorySearcher::category, Function.identity()));
    }

    @Override
    public SearchResult<?> search(String category, String keyword, Pageable pageable) {
        CategorySearcher searcher = searcherMap.get(category);
        if (searcher == null) {
            throw new IllegalArgumentException("不支持的搜索类别: " + category + "，可选: " + searcherMap.keySet());
        }
        PageResult<?> result = searcher.search(keyword, pageable);
        return new SearchResult<>(category, keyword, result);
    }
}
