package com.app.interaction;

import com.app.content.Post;
import com.app.content.PostRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {

    private final CollectionRepository collectionRepository;
    private final PostRepository postRepository;

    @Override
    @Transactional
    @CacheEvict(value = "post", key = "#postId")
    public void collect(Long userId, Long postId) {
        if (collectionRepository.existsByUserIdAndPostId(userId, postId)) {
            throw new IllegalStateException("已经收藏过了");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("帖文不存在"));
        if (post.getStatus() == 0) {
            throw new EntityNotFoundException("帖文已被删除");
        }

        collectionRepository.save(new Collection(userId, postId));
        post.setCollectionsCount(post.getCollectionsCount() + 1);
        postRepository.save(post);

        log.info("Collection created: userId={}, postId={}", userId, postId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "post", key = "#postId")
    public void uncollect(Long userId, Long postId) {
        Collection collection = collectionRepository.findByUserIdAndPostId(userId, postId)
                .orElseThrow(() -> new IllegalStateException("未收藏"));

        collectionRepository.delete(collection);

        Post post = postRepository.findById(postId).orElse(null);
        if (post != null && post.getCollectionsCount() > 0) {
            post.setCollectionsCount(post.getCollectionsCount() - 1);
            postRepository.save(post);
        }

        log.info("Collection removed: userId={}, postId={}", userId, postId);
    }
}
