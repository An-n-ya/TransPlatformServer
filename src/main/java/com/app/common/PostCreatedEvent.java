package com.app.common;

/**
 * 帖文创建事件 — 用于 RabbitMQ 消息
 * @param postId 帖文 ID
 * @param userId 发帖用户 ID
 */
public record PostCreatedEvent(Long postId, Long userId) {
}
