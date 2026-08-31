package com.socialweb.repository;

import com.socialweb.entity.BoilingCommentVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface BoilingCommentVoteRepository extends JpaRepository<BoilingCommentVote, Long> {

    Optional<BoilingCommentVote> findByUserIdAndCommentId(Long userId, Long commentId);

    void deleteByCommentId(Long commentId);

    @Query("select v from BoilingCommentVote v where v.user.id = :uid and v.comment.id in :ids")
    List<BoilingCommentVote> findByUserIdAndCommentIdIn(@Param("uid") Long userId, @Param("ids") Collection<Long> commentIds);
}