package com.app.search;

import com.app.common.PageResult;
import com.app.user.User;
import com.app.user.UserRepository;
import com.app.user.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * 用户类别搜索 — 按 username 或 nickname 模糊匹配
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserCategorySearcher implements CategorySearcher {

    private final UserRepository userRepository;

    @Override
    public String category() {
        return "user";
    }

    @Override
    public PageResult<?> search(String keyword, Pageable pageable) {
        Page<User> page = userRepository.searchByKeyword(keyword, pageable);
        return PageResult.of(
                page.getContent().stream().map(UserVO::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }
}
