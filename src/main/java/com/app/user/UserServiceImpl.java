package com.app.user;

import com.app.common.JwtUtil;
import com.app.common.PageResult;
import com.app.config.RabbitConfig;
import com.app.content.Post;
import com.app.content.PostRepository;
import com.app.email.VerificationCodeService;
import com.app.feed.FollowEventConsumer.FollowEvent;
import com.app.invitation.InvitationService;
import com.app.notification.NotificationRepository;
import com.app.notification.Notification;
import com.app.upload.ImageValidator;
import com.app.upload.StorageService;
import com.app.upload.UploadRequest;
import com.app.upload.UploadResult;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RabbitTemplate rabbitTemplate;
    private final StorageService storageService;
    private final ImageValidator imageValidator;
    private final NotificationRepository notificationRepository;
    private final PostRepository postRepository;
    private final InvitationService invitationService;
    private final VerificationCodeService verificationCodeService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("用户名已被注册");
        }

        User user = new User(
                request.getUsername(),
                request.getNickname(),
                passwordEncoder.encode(request.getPassword())
        );
        user = userRepository.save(user);

        // 校验并消耗邀请码（必须在创建用户之后，记录被邀请人）
        invitationService.validateAndUse(request.getInvitationCode(), user.getId());

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("用户名或密码错误");
        }

        if (user.getStatus() != 1) {
            throw new IllegalStateException("账号已被禁用");
        }

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("无效的 Refresh Token");
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));

        return buildAuthResponse(user);
    }

    @Override
    public boolean isUsernameTaken(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Cacheable(value = "user", key = "#userId", unless = "#result == null")
    public UserVO getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));

        UserVO vo = UserVO.from(user);
        vo.setFollowersCount(followRepository.countByFolloweeIdAndStatus(userId, 1));
        vo.setFolloweesCount(followRepository.countByFollowerIdAndStatus(userId, 1));
        return vo;
    }

    @Override
    @Cacheable(value = "user", key = "#userId", unless = "#result == null")
    public UserVO getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));

        UserVO vo = UserVO.from(user);
        vo.setFollowersCount(followRepository.countByFolloweeIdAndStatus(userId, 1));
        vo.setFolloweesCount(followRepository.countByFollowerIdAndStatus(userId, 1));
        return vo;
    }

    @Override
    @CacheEvict(value = "user", key = "#userId")
    @Transactional
    public UserVO updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));

        Optional.ofNullable(request.getNickname()).ifPresent(user::setNickname);
        Optional.ofNullable(request.getAvatar()).ifPresent(user::setAvatar);
        Optional.ofNullable(request.getBio()).ifPresent(user::setBio);
        Optional.ofNullable(request.getBioHeaderImg()).ifPresent(user::setBioHeaderImg);

        user = userRepository.save(user);
        return UserVO.from(user);
    }

    @Override
    @CacheEvict(value = "user", key = "#userId")
    @Transactional
    public UserVO updateUser(Long userId, String nickname, String bio, MultipartFile bioHeaderImgFile, MultipartFile avatarFile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));

        Optional.ofNullable(nickname).ifPresent(user::setNickname);
        Optional.ofNullable(bio).ifPresent(user::setBio);

        if (bioHeaderImgFile != null && !bioHeaderImgFile.isEmpty()) {
            imageValidator.validate(bioHeaderImgFile);
            try {
                UploadRequest req = new UploadRequest(
                        bioHeaderImgFile.getInputStream(),
                        bioHeaderImgFile.getOriginalFilename(),
                        bioHeaderImgFile.getContentType(),
                        bioHeaderImgFile.getSize(),
                        "bio-headers");
                UploadResult result = storageService.upload(req);
                user.setBioHeaderImg(result.url());
                log.info("Bio header image updated for userId={}, url={}", userId, result.url());
            } catch (IOException e) {
                throw new RuntimeException("主页背景图上传失败", e);
            }
        }

        if (avatarFile != null && !avatarFile.isEmpty()) {
            imageValidator.validate(avatarFile);
            try {
                UploadRequest req = new UploadRequest(
                        avatarFile.getInputStream(),
                        avatarFile.getOriginalFilename(),
                        avatarFile.getContentType(),
                        avatarFile.getSize(),
                        "avatars");
                UploadResult result = storageService.upload(req);
                user.setAvatar(result.url());
                log.info("Avatar updated for userId={}, url={}", userId, result.url());
            } catch (IOException e) {
                throw new RuntimeException("头像上传失败", e);
            }
        }

        user = userRepository.save(user);
        return UserVO.from(user);
    }

    @Override
    @CacheEvict(value = "user", key = "#userId")
    @Transactional
    public UserVO setPinnedPost(Long userId, Long postId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("帖文不存在"));
        if (!post.getUserId().equals(userId)) {
            throw new SecurityException("只能置顶自己的帖文");
        }
        if (post.getStatus() == 0) {
            throw new EntityNotFoundException("帖文已被删除");
        }

        user.setPinnedPostId(postId);
        user = userRepository.save(user);
        log.info("User {} pinned post {}", userId, postId);
        return UserVO.from(user);
    }

    @Override
    @CacheEvict(value = "user", key = "#userId")
    @Transactional
    public UserVO clearPinnedPost(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));

        user.setPinnedPostId(null);
        user = userRepository.save(user);
        log.info("User {} cleared pinned post", userId);
        return UserVO.from(user);
    }

    @Override
    @Transactional
    public void follow(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new IllegalArgumentException("不能关注自己");
        }

        if (!userRepository.existsById(followeeId)) {
            throw new EntityNotFoundException("被关注用户不存在");
        }

        Optional<Follow> existing = followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId);
        if (existing.isPresent()) {
            if (existing.get().getStatus() == 1) {
                throw new IllegalStateException("已经关注了该用户");
            }
            // 重新关注
            existing.get().setStatus(1);
            followRepository.save(existing.get());
            log.info("User {} re-followed user {}", followerId, followeeId);
            return;
        }

        followRepository.save(new Follow(followerId, followeeId));
        log.info("User {} followed user {}", followerId, followeeId);

        if (!followeeId.equals(followerId)) {
            notificationRepository.save(new Notification(
                    followeeId, "follow", "关注了你", null, followerId, null));
        }

        // 异步维护 Feed 列表
        rabbitTemplate.convertAndSend(RabbitConfig.USER_EXCHANGE, RabbitConfig.RK_FOLLOW_CREATED,
                new FollowEvent(followerId, followeeId, "follow"));
    }

    @Override
    @Transactional
    public void unfollow(Long followerId, Long followeeId) {
        Follow follow = followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId)
                .orElseThrow(() -> new IllegalStateException("未关注该用户"));

        if (follow.getStatus() == 0) {
            throw new IllegalStateException("已经取消关注");
        }

        follow.setStatus(0);
        followRepository.save(follow);
        log.info("User {} unfollowed user {}", followerId, followeeId);

        // 异步维护 Feed 列表
        rabbitTemplate.convertAndSend(RabbitConfig.USER_EXCHANGE, RabbitConfig.RK_FOLLOW_CANCELED,
                new FollowEvent(followerId, followeeId, "unfollow"));
    }

    @Override
    public PageResult<UserVO> getFollowers(Long userId, Pageable pageable) {
        Page<Follow> follows = followRepository.findByFolloweeIdAndStatus(userId, 1, pageable);
        List<UserVO> users = follows.getContent().stream()
                .map(f -> {
                    UserVO vo = getUserById(f.getFollowerId());
                    return vo;
                })
                .toList();
        return PageResult.of(users, follows.getNumber(), follows.getSize(), follows.getTotalElements());
    }

    @Override
    public PageResult<UserVO> getFollowees(Long userId, Pageable pageable) {
        Page<Follow> follows = followRepository.findByFollowerIdAndStatus(userId, 1, pageable);
        List<UserVO> users = follows.getContent().stream()
                .map(f -> getUserById(f.getFolloweeId()))
                .toList();
        return PageResult.of(users, follows.getNumber(), follows.getSize(), follows.getTotalElements());
    }

    @Override
    public void sendEmailVerificationCode(Long userId, String email) {
        userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));

        // 邮箱已被其他用户绑定
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("该邮箱已被其他账号绑定");
        }

        verificationCodeService.sendCode(email, VerificationCodeService.SCENE_EMAIL);
    }

    @Override
    @CacheEvict(value = "user", key = "#userId")
    @Transactional
    public UserVO verifyEmail(Long userId, String email, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));

        verificationCodeService.verifyCode(email, VerificationCodeService.SCENE_EMAIL, code);

        // 校验通过后，再次确认邮箱未被他人绑定
        userRepository.findByEmail(email).filter(other -> !other.getId().equals(userId))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("该邮箱已被其他账号绑定");
                });

        user.setEmail(email);
        user = userRepository.save(user);
        log.info("User {} verified and bound email {}", userId, email);
        return UserVO.from(user);
    }

    @Override
    public void sendPasswordResetCode(String email) {
        if (userRepository.findByEmail(email).isEmpty()) {
            throw new IllegalArgumentException("该邮箱未注册");
        }
        verificationCodeService.sendCode(email, VerificationCodeService.SCENE_PASSWORD_RESET);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("该邮箱未注册"));

        verificationCodeService.verifyCode(email, VerificationCodeService.SCENE_PASSWORD_RESET, code);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password reset for user {}", user.getId());
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L) // 24h in seconds
                .user(UserVO.from(user))
                .build();
    }
}
