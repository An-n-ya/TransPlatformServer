package com.app.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 — 定义交换机、队列和绑定关系
 */
@Configuration
public class RabbitConfig {

    // ---------- 交换机 ----------
    public static final String POST_EXCHANGE = "exchange.post";
    public static final String INTERACTION_EXCHANGE = "exchange.interaction";
    public static final String USER_EXCHANGE = "exchange.user";
    public static final String DLX_EXCHANGE = "exchange.dlx";          // 死信交换机
    public static final String DELAY_EXCHANGE = "exchange.delay";      // 延迟交换机

    // ---------- 队列 ----------
    public static final String QUEUE_FEED_PUSH = "queue.feed.push";
    public static final String QUEUE_LIKE_UPDATE = "queue.like.update";
    public static final String QUEUE_COMMENT_UPDATE = "queue.comment.update";
    public static final String QUEUE_FOLLOW_FEED = "queue.follow.feed";
    public static final String QUEUE_NOTIFICATION = "queue.notification";

    // ---------- 路由键 ----------
    public static final String RK_POST_CREATED = "post.created";
    public static final String RK_POST_DELETED = "post.deleted";
    public static final String RK_LIKE_CREATED = "like.created";
    public static final String RK_LIKE_CANCELED = "like.canceled";
    public static final String RK_COMMENT_CREATED = "comment.created";
    public static final String RK_FOLLOW_CREATED = "follow.created";
    public static final String RK_FOLLOW_CANCELED = "follow.canceled";

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    // ========== 交换机定义 ==========
    @Bean
    public DirectExchange postExchange() {
        return new DirectExchange(POST_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange interactionExchange() {
        return new DirectExchange(INTERACTION_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange userExchange() {
        return new DirectExchange(USER_EXCHANGE, true, false);
    }

    // ========== 队列定义 ==========
    @Bean
    public Queue feedPushQueue() {
        return new Queue(QUEUE_FEED_PUSH, true);
    }

    @Bean
    public Queue likeUpdateQueue() {
        return new Queue(QUEUE_LIKE_UPDATE, true);
    }

    @Bean
    public Queue commentUpdateQueue() {
        return new Queue(QUEUE_COMMENT_UPDATE, true);
    }

    @Bean
    public Queue followFeedQueue() {
        return new Queue(QUEUE_FOLLOW_FEED, true);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(QUEUE_NOTIFICATION, true);
    }

    // ========== 绑定关系 ==========
    @Bean
    public Binding feedPushBinding() {
        return BindingBuilder.bind(feedPushQueue()).to(postExchange()).with(RK_POST_CREATED);
    }

    @Bean
    public Binding likeUpdateBinding() {
        return BindingBuilder.bind(likeUpdateQueue()).to(interactionExchange()).with(RK_LIKE_CREATED);
    }

    @Bean
    public Binding likeCancelBinding() {
        return BindingBuilder.bind(likeUpdateQueue()).to(interactionExchange()).with(RK_LIKE_CANCELED);
    }

    @Bean
    public Binding commentUpdateBinding() {
        return BindingBuilder.bind(commentUpdateQueue()).to(interactionExchange()).with(RK_COMMENT_CREATED);
    }

    @Bean
    public Binding followFeedBinding() {
        return BindingBuilder.bind(followFeedQueue()).to(userExchange()).with(RK_FOLLOW_CREATED);
    }

    @Bean
    public Binding followCancelBinding() {
        return BindingBuilder.bind(followFeedQueue()).to(userExchange()).with(RK_FOLLOW_CANCELED);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue()).to(postExchange()).with(RK_POST_CREATED);
    }
}
