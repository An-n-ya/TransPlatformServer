package com.app.notification;

import com.app.common.PageResult;
import org.springframework.data.domain.Pageable;

/**
 * 通知模块 Service 接口
 */
public interface NotificationService {

    /**
     * 创建通知
     */
    void createNotification(Long userId, String type, String title, String content, Long fromUserId, Long targetId);

    /**
     * 获取通知列表（分页，最新在前）
     */
    PageResult<NotificationVO> getNotifications(Long userId, Pageable pageable);

    /**
     * 获取未读通知数
     */
    long getUnreadCount(Long userId);

    /**
     * 标记单条通知为已读
     */
    void markAsRead(Long notificationId, Long userId);

    /**
     * 全部标记已读
     */
    void markAllAsRead(Long userId);
}
