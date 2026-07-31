package com.app.user;

import com.app.common.PageResult;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户模块 Service 接口
 */
public interface UserService {

    /**
     * 用户注册
     * @param request 注册请求
     * @return 认证响应（包含 Token 和用户信息）
     */
    AuthResponse register(RegisterRequest request);

    /**
     * 用户登录
     * @param request 登录请求
     * @return 认证响应
     */
    AuthResponse login(LoginRequest request);

    /**
     * 刷新 Token
     * @param refreshToken 刷新令牌
     * @return 新的认证响应
     */
    AuthResponse refreshToken(String refreshToken);

    /**
     * 获取当前用户信息
     * @param userId 用户 ID
     * @return 用户视图对象
     */
    UserVO getCurrentUser(Long userId);

    /**
     * 获取指定用户信息
     * @param userId 用户 ID
     * @return 用户视图对象
     */
    UserVO getUserById(Long userId);

    /**
     * 更新用户资料（JSON 方式）
     */
    UserVO updateUser(Long userId, UserUpdateRequest request);

    /**
     * 更新用户资料（multipart 方式，支持上传头像）
     *
     * @param userId       用户 ID
     * @param nickname     昵称（可选）
     * @param bio          个人简介（可选）
     * @param bioHeaderImg 主页背景图 URL（可选）
     * @param avatarFile   头像文件（可选，上传后自动替换 avatar URL）
     * @return 更新后的用户视图
     */
    UserVO updateUser(Long userId, String nickname, String bio, String bioHeaderImg, MultipartFile avatarFile);

    /**
     * 设置置顶帖
     * @param userId 用户 ID
     * @param postId 帖文 ID（必须是自己的帖文）
     * @return 更新后的用户视图
     */
    UserVO setPinnedPost(Long userId, Long postId);

    /**
     * 取消置顶帖
     * @param userId 用户 ID
     * @return 更新后的用户视图
     */
    UserVO clearPinnedPost(Long userId);

    /**
     * 关注用户
     * @param followerId 关注者 ID
     * @param followeeId 被关注者 ID
     */
    void follow(Long followerId, Long followeeId);

    /**
     * 取关用户
     * @param followerId 关注者 ID
     * @param followeeId 被关注者 ID
     */
    void unfollow(Long followerId, Long followeeId);

    /**
     * 获取粉丝列表（分页）
     */
    PageResult<UserVO> getFollowers(Long userId, Pageable pageable);

    /**
     * 获取关注列表（分页）
     */
    PageResult<UserVO> getFollowees(Long userId, Pageable pageable);
}
