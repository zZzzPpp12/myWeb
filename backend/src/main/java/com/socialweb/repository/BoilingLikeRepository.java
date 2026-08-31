package com.socialweb.repository;

import com.socialweb.entity.BoilingLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface BoilingLikeRepository extends JpaRepository<BoilingLike, Long> {

    boolean existsByUserIdAndBoilingPointId(Long userId, Long boilingId);

    Optional<BoilingLike> findByUserIdAndBoilingPointId(Long userId, Long boilingId);

    void deleteByBoilingPointId(Long boilingId);

    @Query("select b.boilingPoint.id from BoilingLike b where b.user.id = :uid and b.boilingPoint.id in :ids")
    Set<Long> findLikedBoilingIds(@Param("uid") Long userId, @Param("ids") Collection<Long> boilingIds);
}