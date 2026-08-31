package com.socialweb.repository;

import com.socialweb.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(Long postId, Pageable pageable);

    List<Comment> findByParentIdInOrderByCreatedAtAsc(Collection<Long> parentIds);

    void deleteByPostId(Long postId);

    long countByPostId(Long postId);
}
