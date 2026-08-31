package com.socialweb.config;

import com.socialweb.entity.*;
import com.socialweb.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 首次启动播种演示数据 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserFollowRepository followRepository;
    private final PostLikeRepository likeRepository;
    private final PostBookmarkRepository bookmarkRepository;
    private final NotificationRepository notificationRepository;
    private final TopicRepository topicRepository;
    private final TopicFollowRepository topicFollowRepository;
    private final PostDislikeRepository dislikeRepository;
    private final BoilingPointRepository boilingRepository;
    private final BCryptPasswordEncoder encoder;

    public DataSeeder(UserRepository userRepository,
                      PostRepository postRepository,
                      CommentRepository commentRepository,
                      UserFollowRepository followRepository,
                      PostLikeRepository likeRepository,
                      PostBookmarkRepository bookmarkRepository,
                      NotificationRepository notificationRepository,
                      TopicRepository topicRepository,
                      TopicFollowRepository topicFollowRepository,
                      PostDislikeRepository dislikeRepository,
                      BoilingPointRepository boilingRepository,
                      BCryptPasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.followRepository = followRepository;
        this.likeRepository = likeRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.notificationRepository = notificationRepository;
        this.topicRepository = topicRepository;
        this.topicFollowRepository = topicFollowRepository;
        this.dislikeRepository = dislikeRepository;
        this.boilingRepository = boilingRepository;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("已存在数据，跳过播种");
            return;
        }
        log.info("开始播种演示数据...");
        String pwd = encoder.encode("123456");

        User alice = user("alice", pwd, "爱丽丝", "Java 后端工程师，专注 Spring 生态与高并发架构", 2);
        User bob = user("bob", pwd, "鲍勃", "前端开发，Vue / TypeScript 爱好者", 5);
        User carol = user("carol", pwd, "卡罗尔", "算法与数据结构刷题党，数据库爱好者", 8);
        alice.setReputation(860);
        bob.setReputation(320);
        carol.setReputation(150);
        userRepository.saveAll(List.of(alice, bob, carol));

        // 关注关系：bob、carol 关注 alice；alice 关注 carol
        follow(alice, bob);
        follow(alice, carol);
        follow(bob, alice);
        follow(carol, alice);

        List<Post> posts = new ArrayList<>();
        posts.add(post(alice, "Spring Boot 3 新特性全解析：虚拟线程与 GraalVM", """
                # Spring Boot 3 新特性

                Spring Boot 3 基于 **Spring Framework 6** 与 Java 17，带来了大量改进。

                ## 1. 虚拟线程（Project Loom）

                只需一行配置即可启用：

                ```yaml
                spring:
                  threads:
                    virtual:
                      enabled: true
                ```

                ## 2. GraalVM 原生镜像

                ```bash
                mvn -Pnative native:compile
                ```

                启动时间从秒级降到毫秒级，内存占用显著下降。

                参考资料：[Spring Boot 3 迁移指南](https://spring.io/blog/2022/11/24/annotating-with-spring-boot-3-the-big-rewrite)
                """, PostType.ARTICLE, "Java,Spring,后端", 3, 120, 46, 18, 9));

        posts.add(post(alice, "JVM 调优实战：一次线上 Full GC 频繁排查记录", """
                # Full GC 频繁排查

                现象：老年代占用快速上涨，Full GC 每 10 分钟一次。

                ## 排查步骤

                1. `jstat -gcutil <pid> 1000` 观察 GC 情况
                2. `jmap -histo:live <pid>` 查看大对象
                3. dump 后用 MAT 分析支配树

                ```java
                // 问题代码：ThreadLocal 未清理
                private static final ThreadLocal<byte[]> BUFFER = new ThreadLocal<>();
                ```

                结论：虚拟线程场景下 ThreadLocal 必须显式 `remove()`。
                """, PostType.ARTICLE, "Java,JVM,性能优化", 6, 88, 33, 12, 7));

        posts.add(post(bob, "Vue 3 组合式 API 最佳实践：如何组织大型组件", """
                # Vue 3 组合式 API 实践

                `setup` 让逻辑复用变得自然，但组织不当反而更乱。

                ## 推荐结构

                - `composables/`：可复用逻辑（useAuth、usePagination）
                - `components/`：纯 UI 组件
                - `stores/`：跨页面状态（Pinia）

                ```ts
                export function useCounter(initial = 0) {
                  const count = ref(initial)
                  const inc = () => count.value++
                  return { count, inc }
                }
                ```

                详见 [Vue 文档](https://vuejs.org/guide/reusability/composables.html)
                """, PostType.ARTICLE, "Vue,前端,TypeScript", 2, 150, 52, 21, 11));

        posts.add(post(bob, "前端如何实现 Markdown 渲染且不被 XSS 攻击？", """
                我们在做社区文章功能，需要渲染用户提交的 Markdown。

                直接 `v-html` 显然不安全。目前调研了：

                1. marked + DOMPurify
                2. markdown-it + sanitize 选项
                3. 后端渲染 + CSP

                想问下生产环境大家怎么选型？性能和安全如何权衡？
                """, PostType.QUESTION, "前端,安全,Vue", 4, 60, 25, 6, 4));

        posts.add(post(carol, "MySQL 索引失效的 8 种场景（附验证 SQL）", """
                # 索引失效场景

                ```sql
                -- 1. 函数导致失效
                SELECT * FROM users WHERE YEAR(created_at) = 2024;

                -- 2. 隐式类型转换（字符串列用数字查）
                SELECT * FROM users WHERE username = 12345;

                -- 3. 最左前缀不满足
                -- 4. LIKE 以 % 开头
                -- 5. OR 两侧非同一索引
                ```

                | 场景 | 是否失效 | 说明 |
                | ---- | ---- | ---- |
                | 前缀匹配 | 否 | LIKE 'abc%' 可用 |
                | 覆盖索引 | 否 | 不回表即可 |
                """, PostType.ARTICLE, "数据库,MySQL,后端", 8, 210, 71, 30, 15));

        posts.add(post(carol, "深入理解 Redis 持久化：RDB 与 AOF 怎么选", """
                # RDB vs AOF

                - **RDB**：快照，恢复快，可能丢数据
                - **AOF**：追加日志，数据全，文件大

                ```bash
                save 900 1
                appendonly yes
                appendfsync everysec
                ```

                混合持久化（Redis 4+）是默认推荐方案。
                """, PostType.ARTICLE, "数据库,Redis,后端", 10, 95, 40, 14, 8));

        posts.add(post(alice, "如何设计一个高并发点赞系统？", """
                社区产品里点赞功能看似简单，高并发下问题不少。

                ## 难点

                1. 热点 key（大 V 帖子）
                2. 计数一致性
                3. 防重复点赞

                ## 思路

                - 本地缓存 + 定时合并写 Redis
                - 唯一索引兜底防重
                - 计数用 `INCR` 原子操作

                有没有更好的方案？
                """, PostType.QUESTION, "架构,Java,后端", 12, 76, 28, 9, 6));

        posts.add(post(bob, "算法刷题笔记：动态规划之打家劫舍系列", """
                # 打家劫舍 I/II/III

                状态转移方程：

                ```python
                dp[i] = max(dp[i-1], dp[i-2] + nums[i])
                ```

                - I：线性
                - II：环形 -> 分两种情况取 max
                - III：树形 -> 树形 DP

                核心是**状态定义**，定义对了转移自然出来。
                """, PostType.ARTICLE, "算法,LeetCode,Python", 14, 64, 22, 8, 5));

        posts.add(post(carol, "PostgreSQL 与 MySQL 在 JSON 支持上的差异实测", """
                实测 JSON 类型读写性能：

                | 维度 | MySQL 8 | PostgreSQL 15 |
                | ---- | ---- | ---- |
                | JSON 类型 | 伪 JSON | 原生 jsonb |
                | 索引 | 函数索引 | GIN 索引 |
                | 更新 | 整行重写 | 原地更新 |

                ```sql
                -- PG 的 jsonb 查询
                SELECT * FROM events WHERE payload @> '{"type": "click"}';
                ```
                """, PostType.ARTICLE, "数据库,PostgreSQL,MySQL", 16, 58, 19, 7, 3));

        postRepository.saveAll(posts);

        // 反对关系（知乎式赞同/反对双向评价）
        posts.get(3).setDislikeCount(1);
        posts.get(6).setDislikeCount(1);
        postRepository.saveAll(List.of(posts.get(3), posts.get(6)));
        dislike(alice, posts.get(3));
        dislike(bob, posts.get(6));

        // 话题体系
        Topic java = topic("Java", "Java 语言、Spring 生态与 JVM 调优", "☕");
        Topic vue = topic("Vue", "Vue 3、组合式 API 与前端工程化", "🖖");
        Topic fe = topic("前端", "前端开发、性能与安全", "💻");
        Topic be = topic("后端", "服务端架构、微服务与中间件", "⚙️");
        Topic db = topic("数据库", "MySQL/Redis/PostgreSQL 实战", "🗄");
        Topic algo = topic("算法", "数据结构与算法刷题札记", "🧮");
        Topic arch = topic("架构", "分布式与高并发架构设计", "🏗");
        Topic sec = topic("安全", "Web 安全与 XSS/CSRF 防护", "🔐");
        topicRepository.saveAll(List.of(java, vue, fe, be, db, algo, arch, sec));
        topicFollow(alice, java); topicFollow(alice, be); topicFollow(alice, arch);
        topicFollow(bob, vue); topicFollow(bob, fe); topicFollow(bob, sec);
        topicFollow(carol, db); topicFollow(carol, algo); topicFollow(carol, be);

        // 沸点（掘金式短内容）
        boil(alice, "刚把生产环境的 Full GC 问题解决了，改天写篇复盘。", null, 12, 15);
        boil(bob, "Vue 3.5 的响应式重构真好用，`useTemplateRef` 好评。", null, 8, 45);
        boil(carol, "刷了 100 道动态规划，状态转移方程终于有感觉了。", null, 5, 90);
        boil(alice, "#架构 为什么说「缓存和数据库一致性」是个伪命题？", null, 3, 180);
        boil(bob, "分享一个 Markdown 渲染 XSS 的坑，详见我主页的文章。", null, 2, 240);

        // 评论（含一级回复）
        comment(posts.get(0), bob, "虚拟线程这块太香了，实测 IO 密集接口 QPS 翻倍！", null, 1);
        comment(posts.get(0), carol, "GraalVM 的反射配置还是有点麻烦，不过 AOT 收益明显。", null, 2);
        comment(posts.get(0), alice, "同意，我们项目已经全量切了。", 1L, 3);
        comment(posts.get(4), alice, "最左前缀这个坑我踩过，组合索引顺序很关键。", null, 4);
        comment(posts.get(4), bob, "覆盖索引那段讲得清楚，收藏了。", null, 5);
        comment(posts.get(2), carol, "composables 拆分确实让组件清爽很多。", null, 6);
        comment(posts.get(3), alice, "推荐后端渲染 + DOMPurify 双保险。", null, 7);
        comment(posts.get(6), carol, "本地缓存合并写是主流方案，我们线上也这么干。", null, 8);

        // 点赞关系
        like(alice, posts.get(2)); like(alice, posts.get(4)); like(alice, posts.get(7));
        like(bob, posts.get(0)); like(bob, posts.get(1)); like(bob, posts.get(4));
        like(carol, posts.get(0)); like(carol, posts.get(2)); like(carol, posts.get(5));

        // 收藏
        bookmark(alice, posts.get(4));
        bookmark(bob, posts.get(0));
        bookmark(carol, posts.get(7));

        // 通知
        notify(posts.get(0).getAuthor(), bob, NotificationType.LIKE, posts.get(0).getId());
        notify(posts.get(0).getAuthor(), carol, NotificationType.COMMENT, posts.get(0).getId());
        notify(posts.get(4).getAuthor(), alice, NotificationType.COMMENT, posts.get(4).getId());
        notify(alice, bob, NotificationType.FOLLOW, null);
        notify(alice, carol, NotificationType.BOILING_LIKE, null);

        log.info("播种完成：{} 用户 / {} 文章 / {} 话题 / {} 沸点",
                userRepository.count(), postRepository.count(), topicRepository.count(), boilingRepository.count());
    }

    private User user(String username, String pwd, String nickname, String bio, int seed) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(pwd);
        u.setNickname(nickname);
        u.setBio(bio);
        u.setRole(Role.USER);
        u.setAvatar("https://api.dicebear.com/7.x/thumbs/svg?seed=" + username);
        u.setCreatedAt(LocalDateTime.now().minusDays(30 + seed));
        return u;
    }

    private Post post(User author, String title, String content, PostType type,
                      String tags, long hoursAgo, long views, long likes, long comments, long marks) {
        Post p = new Post();
        p.setAuthor(author);
        p.setTitle(title);
        p.setContent(content);
        p.setType(type);
        p.setTags(tags);
        p.setViewCount(views);
        p.setLikeCount(likes);
        p.setCommentCount(comments);
        p.setBookmarkCount(marks);
        p.setCreatedAt(LocalDateTime.now().minusHours(hoursAgo));
        p.setUpdatedAt(LocalDateTime.now().minusHours(hoursAgo));
        return p;
    }

    private Comment comment(Post post, User author, String content, Long parentId, long hoursAgo) {
        Comment c = new Comment();
        c.setPost(post);
        c.setAuthor(author);
        c.setContent(content);
        c.setParentId(parentId);
        c.setCreatedAt(LocalDateTime.now().minusHours(hoursAgo));
        return commentRepository.save(c);
    }

    private void follow(User follower, User followee) {
        UserFollow f = new UserFollow();
        f.setFollower(follower);
        f.setFollowee(followee);
        followRepository.save(f);
    }

    private void like(User u, Post p) {
        PostLike l = new PostLike();
        l.setUser(u);
        l.setPost(p);
        likeRepository.save(l);
    }

    private void bookmark(User u, Post p) {
        PostBookmark b = new PostBookmark();
        b.setUser(u);
        b.setPost(p);
        bookmarkRepository.save(b);
    }

    private Topic topic(String name, String desc, String emoji) {
        Topic t = new Topic();
        t.setName(name);
        t.setDescription(desc);
        t.setEmoji(emoji);
        return t;
    }

    private void topicFollow(User u, Topic t) {
        TopicFollow tf = new TopicFollow();
        tf.setUser(u);
        tf.setTopic(t);
        topicFollowRepository.save(tf);
        t.setFollowerCount(t.getFollowerCount() + 1);
    }

    private void dislike(User u, Post p) {
        PostDislike d = new PostDislike();
        d.setUser(u);
        d.setPost(p);
        dislikeRepository.save(d);
    }

    private void boil(User author, String content, String imageUrl, long likeCount, long minutesAgo) {
        BoilingPoint b = new BoilingPoint();
        b.setAuthor(author);
        b.setContent(content);
        b.setImageUrl(imageUrl);
        b.setLikeCount(likeCount);
        b.setCreatedAt(LocalDateTime.now().minusMinutes(minutesAgo));
        boilingRepository.save(b);
    }

    private void notify(User recipient, User actor, NotificationType type, Long postId) {
        Notification n = new Notification();
        n.setUser(recipient);
        n.setActor(actor);
        n.setType(type);
        n.setPostId(postId);
        notificationRepository.save(n);
    }
}
