package com.socialweb.repository;

import com.socialweb.entity.BoilingComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BoilingCommentRepository extends JpaRepository<BoilingComment, Long> {

    Page<BoilingComment> findByBoilingPointIdAndParentIdIsNull(Long boilingId, Pageable pageable);

    List<BoilingComment> findByBoilingPointId(Long boilingId);

    List<BoilingComment> findByParentIdIn(Collection<Long> parentIds);

    void deleteByBoilingPointId(Long boilingId);

    long countByBoilingPointId(Long boilingId);
}