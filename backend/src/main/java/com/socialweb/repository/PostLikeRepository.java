package com.socialweb.repository;

import com.socialweb.entity.PostLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    Optional<PostLike> findByUserIdAndPostId(Long userId, Long postId);

    void deleteByPostId(Long postId);

    Page<PostLike> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("select l.post.id from PostLike l where l.user.id = :uid and l.post.id in :ids")
    Set<Long> findLikedPostIds(@Param("uid") Long userId, @Param("ids") Collection<Long> postIds);

    List<PostLike> findByUserId(Long userId);
}
