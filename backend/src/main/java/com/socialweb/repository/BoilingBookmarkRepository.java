package com.socialweb.repository;

import com.socialweb.entity.BoilingBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BoilingBookmarkRepository extends JpaRepository<BoilingBookmark, Long> {

    Optional<BoilingBookmark> findByUserIdAndBoilingPointId(Long userId, Long boilingId);

    void deleteByBoilingPointId(Long boilingId);

    @Query("select b.boilingPoint.id from BoilingBookmark b where b.user.id = :uid and b.boilingPoint.id in :ids")
    Set<Long> findBookmarkedBoilingIds(@Param("uid") Long userId, @Param("ids") Collection<Long> boilingIds);

    @Query("select b.boilingPoint.id from BoilingBookmark b where b.user.id = :uid order by b.createdAt desc")
    List<Long> findBookmarkedBoilingIdsOrderByCreatedDesc(@Param("uid") Long userId);
}