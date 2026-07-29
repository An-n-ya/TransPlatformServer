package com.app.content;

import com.app.common.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 内容模块 Service 接口
 */
public interface PostService {

    /**
     * 发布帖文
     * @param userId  用户 ID
     * @param request 创建请求
     * @return 帖文视图
     */
    PostVO createPost(Long userId, PostCreateRequest request);

    /**
     * 获取帖文详情
     * @param postId 帖文 ID
     * @param currentUserId 当前用户 ID（可为 null，用于判断是否点赞/收藏）
     * @return 帖文详情视图
     */
    PostVO getPost(Long postId, Long currentUserId);

    /**
     * 删除帖文（逻辑删除）
     */
    void deletePost(Long postId, Long currentUserId);

    /**
     * 获取指定用户的帖文列表（分页）
     */
    PageResult<PostVO> getUserPosts(Long userId, Long currentUserId, Pageable pageable);

    /**
     * 根据ID批量查询帖文（用于 Feed 流）
     */
    List<PostVO> getPostsByIds(List<Long> ids, Long currentUserId);
}
