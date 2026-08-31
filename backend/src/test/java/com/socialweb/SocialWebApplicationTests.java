package com.socialweb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SocialWebApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
        assertNotNull(mockMvc);
    }

    @Test
    void registerAndLoginSmoke() throws Exception {
        String username = "smoketest_" + System.currentTimeMillis();
        String body = objectMapper.writeValueAsString(
                Map.of("username", username, "password", "123456", "nickname", "冒烟测试"));

        MvcResult reg = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value(username))
                .andReturn();

        JsonNode regJson = objectMapper.readTree(reg.getResponse().getContentAsString());
        String token = regJson.get("token").asText();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        JsonNode loginJson = objectMapper.readTree(login.getResponse().getContentAsString());
        assertEquals(regJson.get("user").get("id").asLong(), loginJson.get("user").get("id").asLong());

        // 错误密码 -> 401
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", "wrongpwd"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicListWithoutAuthHasFalseFlags() throws Exception {
        // 先注册一篇文章，再匿名访问列表，liked/bookmarked 应为 false
        String username = "anonuser_" + System.currentTimeMillis();
        String reg = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", "123456", "nickname", "匿名视角"))))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(reg).get("token").asText();

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("title", "测试文章", "content", "正文内容", "type", "ARTICLE", "tags", List.of("Java")))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.content[0].liked").value(false))
                .andExpect(jsonPath("$.content[0].bookmarked").value(false));
    }
}
