package com.socialweb.repository;

import com.socialweb.entity.PostDislike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PostDislikeRepository extends JpaRepository<PostDislike, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    Optional<PostDislike> findByUserIdAndPostId(Long userId, Long postId);

    void deleteByPostId(Long postId);

    List<PostDislike> findByUserId(Long userId);

    @Query("select d.post.id from PostDislike d where d.user.id = :uid and d.post.id in :ids")
    Set<Long> findDislikedPostIds(@Param("uid") Long userId, @Param("ids") Collection<Long> postIds);
}