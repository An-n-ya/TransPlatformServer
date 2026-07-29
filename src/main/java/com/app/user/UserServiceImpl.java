package com.app.user;

import com.app.common.JwtUtil;
import com.app.common.PageResult;
import com.app.config.RabbitConfig;
import com.app.feed.FollowEventConsumer.FollowEvent;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
