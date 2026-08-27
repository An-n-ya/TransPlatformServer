package com.app.user;

import com.app.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户实体
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(nullable = false)
    private String password;

    @Column(length = 500)
    private String avatar;

    @Column(length = 200)
    private String bio;

    /** 用户位置（城市，用于“附近”时间流过滤） */
    @Column(length = 200)
    private String location;

    @Column(name = "bio_header_img", length = 500)
    private String bioHeaderImg;

    @Column(length = 255)
    private String email;

    @Column(name = "pinned_post_id")
    private Long pinnedPostId;

    @Column(nullable = false, length = 20)
    private String role = "user";

    @Column(nullable = false)
    private Integer status = 1;

    public User(String username, String nickname, String password) {
        this.username = username;
        this.nickname = nickname;
        this.password = password;
        this.role = "user";
        this.status = 1;
    }
}
