package com.app.notification;

import com.app.common.PageResult;
import com.app.user.UserService;
import com.app.user.UserVO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;

    @Override
    @Transactional
    public void createNotification(Long userId, String type, String title, String content, Long fromUserId, Long targetId) {
        if (userId.equals(fromUserId)) {
            return; // 不给自己的操作发通知
        }
        Notification notification = new Notification(userId, type, title, content, fromUserId, targetId);
        notificationRepository.save(notification);
        log.debug("Notification created: userId={}, type={}, fromUserId={}", userId, type, fromUserId);
    }

    @Override
    public PageResult<NotificationVO> getNotifications(Long userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<NotificationVO> vos = page.getContent().stream()
                .map(n -> {
                    UserVO fromUser = n.getFromUserId() != null
                            ? userService.getUserById(n.getFromUserId()) : null;
                    return NotificationVO.from(n, fromUser);
                })
                .toList();
        return PageResult.of(vos, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, 0);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("通知不存在"));
        if (!notification.getUserId().equals(userId)) {
            throw new SecurityException("无权操作他人通知");
        }
        notification.setIsRead(1);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        int count = notificationRepository.markAllAsRead(userId);
        log.debug("Marked {} notifications as read for userId={}", count, userId);
    }
}
