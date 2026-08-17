package com.app.topic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 话题视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "话题视图对象")
public class TopicVO {

    private Long id;
    private String name;
    private String description;
    private Long postCount;
    private Long participantCount;
    private LocalDateTime createdAt;

    public static TopicVO from(Topic topic, Long postCount) {
        return TopicVO.builder()
                .id(topic.getId())
                .name(topic.getName())
                .description(topic.getDescription())
                .postCount(postCount)
                .createdAt(topic.getCreatedAt())
                .build();
    }
}
