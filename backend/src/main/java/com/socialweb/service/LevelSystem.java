package com.socialweb.service;

import com.socialweb.entity.User;

/** 声望与等级体系（融合知乎盐值与掘金掘力值，自定义等级称号） */
public final class LevelSystem {

    public static final int POST_CREATE = 10;
    public static final int BOILING_CREATE = 5;
    public static final int RECEIVE_LIKE = 5;
    public static final int RECEIVE_FOLLOW = 3;
    public static final int COMMENT_CREATE = 2;
    public static final int RECEIVE_DISLIKE = -2;

    private LevelSystem() {
    }

    public static int level(int reputation) {
        if (reputation < 50) return 1;
        if (reputation < 200) return 2;
        if (reputation < 500) return 3;
        if (reputation < 1000) return 4;
        if (reputation < 2000) return 5;
        return 6;
    }

    public static String levelName(int reputation) {
        return switch (level(reputation)) {
            case 2 -> "新锐";
            case 3 -> "进阶";
            case 4 -> "资深";
            case 5 -> "专家";
            case 6 -> "大师";
            default -> "见习";
        };
    }

    /** 累加声望，下限 0 */
    public static void award(User u, int delta) {
        if (u == null) return;
        u.setReputation(Math.max(0, u.getReputation() + delta));
    }
}