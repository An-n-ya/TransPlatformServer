package com.app.content;

import com.app.common.PageResult;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 内容模块 Service 接口
 */
public interface PostService {

    /**
     * 发布帖文（JSON 方式，图片需先通过上传接口获取 URL）
     */
    PostVO createPost(Long userId, PostCreateRequest request);

    /**
     * 发布帖文（multipart 方式，直接上传图片文件）
     *
     * @param userId   用户 ID
     * @param content  文字内容
     * @param location 发布位置（可选）
     * @param topicIds 话题 ID 列表（可选）
     * @param images   图片文件列表（可选）
     * @return 帖文视图
     */
    PostVO createPost(Long userId, String content, String location, List<Long> topicIds, List<MultipartFile> images);

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
     * 统一查询帖文（JSON 参数：postId / userId / topicId / content）
     *
     * @param query         查询条件（postId / userId / topicId / content），
     *                     content 存在时必须提供 userId；至少提供一个查询参数
     * @param currentUserId 当前登录用户（用于判断点赞/收藏，可为 null）
     * @param pageable      分页参数
     */
    PageResult<PostVO> queryPosts(PostQueryRequest query, Long currentUserId, Pageable pageable);

    /**
     * 根据ID批量查询帖文（用于 Feed 流）
     */
    List<PostVO> getPostsByIds(List<Long> ids, Long currentUserId);
}
