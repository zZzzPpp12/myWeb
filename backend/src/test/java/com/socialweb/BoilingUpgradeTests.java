package com.socialweb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 沸点模块升级：社交互动 + 讨论 + 埋点 集成测试 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BoilingUpgradeTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String login(String username) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", "123456"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void p1_userSummaryHasOnlineFlag() throws Exception {
        String token = login("alice");
        // 登录后 touchActive 已生效，alice 应在线
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.online").value(true))
                .andExpect(jsonPath("$.likesReceived").isNumber())
                .andExpect(jsonPath("$.boilingsCount").isNumber());
    }

    @Test
    void p2_createWithMentionAndCircle() throws Exception {
        String token = login("alice");
        MvcResult r = mockMvc.perform(post("/api/boiling")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "测试 @bob 提及功能", "circle", "后端"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.circle").value("后端"))
                .andReturn();
        long boilingId = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();

        // bob 应收到 MENTION 通知
        String bobToken = login("bob");
        MvcResult notif = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode content = objectMapper.readTree(notif.getResponse().getContentAsString()).get("content");
        boolean hasMention = false;
        for (JsonNode n : content) {
            if ("MENTION".equals(n.get("type").asText())) hasMention = true;
        }
        assertTrue(hasMention, "bob 应收到 MENTION 通知");

        // 圈子过滤
        mockMvc.perform(get("/api/boiling").param("circle", "后端"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].circle").value("后端"));
    }

    @Test
    void p3_commentThreadVoteReportSort() throws Exception {
        String alice = login("alice");
        String bob = login("bob");

        // alice 发沸点
        MvcResult r = mockMvc.perform(post("/api/boiling")
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "讨论测试帖"))))
                .andExpect(status().isOk())
                .andReturn();
        long bid = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();

        // bob 主评论
        MvcResult c1 = mockMvc.perform(post("/api/boiling/" + bid + "/comments")
                        .header("Authorization", "Bearer " + bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "主评论"))))
                .andExpect(status().isOk())
                .andReturn();
        long cid = objectMapper.readTree(c1.getResponse().getContentAsString()).get("id").asLong();

        // alice 嵌套回复
        mockMvc.perform(post("/api/boiling/" + bid + "/comments")
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "嵌套回复", "parentId", cid))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId").value(cid))
                .andExpect(jsonPath("$.replyToUser.username").value("bob"));

        // 评论投票：赞 +1，再点取消归零
        mockMvc.perform(post("/api/boiling/comments/" + cid + "/vote")
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("up", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myVote").value(true))
                .andExpect(jsonPath("$.likeCount").value(1));
        mockMvc.perform(post("/api/boiling/comments/" + cid + "/vote")
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("up", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myVote").doesNotExist())
                .andExpect(jsonPath("$.likeCount").value(0));

        // 排序参数不报错
        mockMvc.perform(get("/api/boiling/" + bid + "/comments").param("sort", "hot"))
                .andExpect(status().isOk());

        // 举报评论
        mockMvc.perform(post("/api/boiling/BOILING_COMMENT/" + cid + "/report")
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "测试举报"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reported").value(true));
    }

    @Test
    void p4_bookmarkShareFeaturedAnalytics() throws Exception {
        String alice = login("alice");
        String bob = login("bob");

        // 收藏 toggle
        mockMvc.perform(post("/api/boiling/1/bookmark").header("Authorization", "Bearer " + bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookmarked").value(true));

        // 转发计数
        mockMvc.perform(post("/api/boiling/1/share").header("Authorization", "Bearer " + bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareCount").isNumber());

        // 精选沸点
        mockMvc.perform(get("/api/boiling/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 我的圈子
        mockMvc.perform(get("/api/boiling/circles").header("Authorization", "Bearer " + alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 埋点上报（匿名）
        mockMvc.perform(post("/api/boiling/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("action", "boiling_view", "targetType", "boiling", "targetId", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        // 统计汇总
        mockMvc.perform(get("/api/boiling/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boiling_view").isNumber());
    }
}