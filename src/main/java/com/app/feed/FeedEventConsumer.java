package com.app.feed;

import com.app.common.PostCreatedEvent;
import com.app.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Feed 流事件消费者 — 异步处理帖文推送与 Feed 维护
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedEventConsumer {

    private final FeedService feedService;

    /**
     * 处理帖文创建事件：推送到所有粉丝的 Feed 列表
     */
    @RabbitListener(queues = RabbitConfig.QUEUE_FEED_PUSH)
    public void handlePostCreated(PostCreatedEvent event) {
        log.info("Received feed push event: postId={}, userId={}", event.postId(), event.userId());
        try {
            feedService.pushPostToFollowers(event.postId(), event.userId());
        } catch (Exception e) {
            log.error("Failed to push post {} to followers", event.postId(), e);
        }
    }
}
