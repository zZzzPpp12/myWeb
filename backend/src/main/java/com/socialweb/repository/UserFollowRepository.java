package com.socialweb.repository;

import com.socialweb.entity.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    void deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    long countByFollowerId(Long followerId);

    long countByFolloweeId(Long followeeId);

    Page<UserFollow> findByFolloweeId(Long followeeId, Pageable pageable);

    Page<UserFollow> findByFollowerId(Long followerId, Pageable pageable);

    List<UserFollow> findByFollowerId(Long followerId);
}
