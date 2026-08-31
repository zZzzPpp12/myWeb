package com.socialweb.repository;

import com.socialweb.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    long countByAuthorId(Long authorId);

    @org.springframework.data.jpa.repository.Query("select coalesce(sum(p.likeCount), 0) from Post p where p.author.id = :authorId")
    long sumLikeCountByAuthorId(@org.springframework.data.repository.query.Param("authorId") Long authorId);

    Page<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    List<Post> findByIdIn(Collection<Long> ids);
}
