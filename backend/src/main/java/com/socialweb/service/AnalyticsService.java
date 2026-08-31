package com.socialweb.service;

import com.socialweb.entity.AnalyticsEvent;
import com.socialweb.repository.AnalyticsEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 用户行为埋点：记录互动事件并输出统计指标 */
@Service
public class AnalyticsService {

    private final AnalyticsEventRepository repository;

    public AnalyticsService(AnalyticsEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void track(Long userId, String action, String targetType, Long targetId, String extra) {
        AnalyticsEvent e = new AnalyticsEvent();
        e.setUserId(userId);
        e.setAction(action);
        e.setTargetType(targetType);
        e.setTargetId(targetId);
        e.setExtra(truncate(extra, 500));
        repository.save(e);
    }

    /** 按事件名统计（管理/评估用） */
    @Transactional(readOnly = true)
    public Map<String, Long> summary() {
        Map<String, Long> m = new LinkedHashMap<>();
        for (Object[] row : repository.countGroupByAction()) {
            m.put((String) row[0], (Long) row[1]);
        }
        return m;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}