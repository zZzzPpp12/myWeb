package com.socialweb.repository;

import com.socialweb.entity.TopicFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TopicFollowRepository extends JpaRepository<TopicFollow, Long> {

    boolean existsByUserIdAndTopicId(Long userId, Long topicId);

    Optional<TopicFollow> findByUserIdAndTopicId(Long userId, Long topicId);

    void deleteByUserIdAndTopicId(Long userId, Long topicId);

    long countByTopicId(Long topicId);

    List<TopicFollow> findByUserId(Long userId);

    @Query("select tf.topic.name from TopicFollow tf where tf.user.id = :uid")
    List<String> findTopicNamesByUserId(@Param("uid") Long userId);
}