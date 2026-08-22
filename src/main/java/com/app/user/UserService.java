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
     * 校验用户名是否已被占用
     * @param username 待校验用户名
     * @return true 已被占用
     */
    boolean isUsernameTaken(String username);

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
     *
     * @param userId  用户 ID
     * @param request 更新请求（昵称/头像/简介/背景图，均可选）
     * @return 更新后的用户视图
     */
    UserVO updateUser(Long userId, UserUpdateRequest request);

    /**
     * 更新用户资料（multipart 方式，支持上传头像和主页背景图）
     *
     * @param userId           用户 ID
     * @param nickname         昵称（可选）
     * @param bio              个人简介（可选）
     * @param bioHeaderImgFile 主页背景图文件（可选，上传后自动替换 bioHeaderImg URL）
     * @param avatarFile       头像文件（可选，上传后自动替换 avatar URL）
     * @return 更新后的用户视图
     */
    UserVO updateUser(Long userId, String nickname, String bio, MultipartFile bioHeaderImgFile, MultipartFile avatarFile);

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

    /**
     * 发送邮箱验证码（验证/绑定邮箱）
     *
     * @param userId 当前用户 ID
     * @param email  待验证邮箱
     */
    void sendEmailVerificationCode(Long userId, String email);

    /**
     * 校验验证码并绑定邮箱到当前用户
     *
     * @param userId 当前用户 ID
     * @param email  待绑定邮箱
     * @param code   验证码
     * @return 更新后的用户视图
     */
    UserVO verifyEmail(Long userId, String email, String code);

    /**
     * 发送找回密码验证码
     *
     * @param email 已注册邮箱
     */
    void sendPasswordResetCode(String email);

    /**
     * 校验验证码并重置密码
     *
     * @param email       已注册邮箱
     * @param code        验证码
     * @param newPassword 新密码
     */
    void resetPassword(String email, String code, String newPassword);
}
