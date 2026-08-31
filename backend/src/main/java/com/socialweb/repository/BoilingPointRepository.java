package com.socialweb.repository;

import com.socialweb.entity.BoilingPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoilingPointRepository extends JpaRepository<BoilingPoint, Long> {

    long countByAuthorId(Long authorId);

    List<BoilingPoint> findByAuthorIdOrderByCreatedAtDesc(Long authorId);
}