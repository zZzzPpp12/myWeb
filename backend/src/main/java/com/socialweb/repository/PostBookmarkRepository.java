package com.socialweb.repository;

import com.socialweb.entity.PostBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PostBookmarkRepository extends JpaRepository<PostBookmark, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    Optional<PostBookmark> findByUserIdAndPostId(Long userId, Long postId);

    void deleteByPostId(Long postId);

    void deleteByUserIdAndPostId(Long userId, Long postId);

    Page<PostBookmark> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("select b.post.id from PostBookmark b where b.user.id = :uid and b.post.id in :ids")
    Set<Long> findBookmarkedPostIds(@Param("uid") Long userId, @Param("ids") Collection<Long> postIds);

    List<PostBookmark> findByUserId(Long userId);
}
