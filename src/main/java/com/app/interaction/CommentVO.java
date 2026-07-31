package com.app.interaction;

import com.app.user.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "评论视图对象")
public class CommentVO {

    private Long id;
    private Long postId;
    private UserVO author;
    private Long parentId;
    private UserVO replyToUser;
    private String content;
    private Integer likesCount;
    private Long commentsCount;
    private CommentTopReply topReply;
    private List<CommentVO> replies;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "精选回复预览")
    public static class CommentTopReply {
        private Long id;
        private Long userId;
        private String nickname;
        private String content;
        private Integer likesCount;
    }
}
