package com.app.topic;

import com.app.common.PageResult;
import com.app.search.CategorySearcher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * 话题类别搜索 — 按名称模糊匹配
 */
@Component
@RequiredArgsConstructor
public class TopicCategorySearcher implements CategorySearcher {

    private final TopicService topicService;

    @Override
    public String category() {
        return "topic";
    }

    @Override
    public PageResult<?> search(String keyword, Pageable pageable) {
        return topicService.searchTopics(keyword, pageable);
    }
}
