package com.app.topic;

import com.app.common.PageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 话题模块 Service 接口
 */
public interface TopicService {

    /**
     * 创建话题
     */
    TopicVO createTopic(TopicRequest request);

    /**
     * 更新话题
     */
    TopicVO updateTopic(Long topicId, TopicRequest request);

    /**
     * 删除话题（同时清理帖文关联）
     */
    void deleteTopic(Long topicId);

    /**
     * 获取话题详情
     */
    TopicVO getTopic(Long topicId);

    /**
     * 话题列表（分页）
     */
    PageResult<TopicVO> listTopics(Pageable pageable);

    /**
     * 按名称模糊搜索话题
     */
    PageResult<TopicVO> searchTopics(String keyword, Pageable pageable);

    /**
     * 热门话题：按参与人数（去重用户数）最多的前 N 个
     * @param limit 返回数量（默认 10）
     */
    List<TopicVO> getHotTopics(int limit);
}
