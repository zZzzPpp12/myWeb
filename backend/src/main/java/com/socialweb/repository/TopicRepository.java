package com.socialweb.repository;

import com.socialweb.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    Optional<Topic> findByName(String name);

    boolean existsByName(String name);
}