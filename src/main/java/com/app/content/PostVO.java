package com.app.content;

import com.app.topic.TopicVO;
import com.app.user.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖文视图对象（列表用，不含完整评论）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "帖文视图对象")
public class PostVO {

    private Long id;
    private UserVO author;
    private String content;
    private List<String> images;
    private String location;
    private Integer likesCount;
    private Integer commentsCount;
    private Integer collectionsCount;
    private List<TopicVO> topics;
    private Boolean liked;
    private Boolean collected;
    private LocalDateTime createdAt;
}
