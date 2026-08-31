package com.socialweb.repository;

import com.socialweb.entity.AnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {

    long countByAction(String action);

    @Query("select a.action, count(a) from AnalyticsEvent a group by a.action order by count(a) desc")
    List<Object[]> countGroupByAction();
}