package com.app.content;

import com.app.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 帖文实体
 */
@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
public class Post extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "JSON")
    private String images;

    @Column(length = 200)
    private String location;

    @Column(name = "likes_count", nullable = false)
    private Integer likesCount = 0;

    @Column(name = "comments_count", nullable = false)
    private Integer commentsCount = 0;

    @Column(name = "collections_count", nullable = false)
    private Integer collectionsCount = 0;

    @Column(nullable = false)
    private Integer status = 1;

    public Post(Long userId, String content, String images, String location) {
        this.userId = userId;
        this.content = content;
        this.images = images;
        this.location = location;
        this.status = 1;
        this.likesCount = 0;
        this.commentsCount = 0;
        this.collectionsCount = 0;
    }
}
