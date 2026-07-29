package com.app.feed;

import com.app.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 关注事件消费者 — 异步维护 Feed 列表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FollowEventConsumer {

    private final FeedService feedService;

    @RabbitListener(queues = RabbitConfig.QUEUE_FOLLOW_FEED)
    public void handleFollowCreated(FollowEvent event) {
        log.info("Received follow event: followerId={}, followeeId={}, action={}",
                event.followerId(), event.followeeId(), event.action());
        try {
            if ("follow".equals(event.action())) {
                feedService.pullFolloweePosts(event.followerId(), event.followeeId());
            } else if ("unfollow".equals(event.action())) {
                feedService.removeFolloweePosts(event.followerId(), event.followeeId());
            }
        } catch (Exception e) {
            log.error("Failed to process follow event", e);
        }
    }

    public record FollowEvent(Long followerId, Long followeeId, String action) {}
}
