package com.app.config;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 聊天处理器（桩实现）
 *
 * TODO: 实现完整的私信处理逻辑:
 * - 连接时验证 JWT Token
 * - 维护 userId → WebSocketSession 映射
 * - 消息路由到在线用户
 * - 离线消息持久化到数据库
 */
public class ChatWebSocketHandler extends TextWebSocketHandler {

    /** 在线用户会话映射 */
    private final ConcurrentHashMap<Long, WebSocketSession> onlineSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // TODO: 从 session.getAttributes() 或 query params 获取 userId
        // Long userId = extractUserId(session);
        // onlineSessions.put(userId, session);
        System.out.println("WebSocket connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // TODO: 解析消息、路由到目标用户、持久化
        System.out.println("Received message: " + message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // TODO: 移除在线会话
        // onlineSessions.values().remove(session);
        System.out.println("WebSocket disconnected: " + session.getId() + ", status: " + status);
    }
}
