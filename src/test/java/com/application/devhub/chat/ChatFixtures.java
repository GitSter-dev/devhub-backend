package com.application.devhub.chat;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

final class ChatFixtures {

    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper;

    ChatFixtures(MockMvc mockMvc, JsonMapper jsonMapper) {
        this.mockMvc = mockMvc;
        this.jsonMapper = jsonMapper;
    }

    ResultActions openDirect(String bearer, UUID userId) throws Exception {
        return mockMvc.perform(post("/conversations/direct").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"userId\": \"" + userId + "\"}"));
    }

    UUID direct(String bearer, UUID userId) throws Exception {
        return UUID.fromString(data(openDirect(bearer, userId)).get("id").asString());
    }

    ResultActions createGroup(String bearer, String title, List<UUID> members) throws Exception {
        String ids = members.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(","));
        return mockMvc.perform(post("/conversations/groups").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"" + title + "\", \"memberIds\": [" + ids + "]}"));
    }

    UUID group(String bearer, String title, List<UUID> members) throws Exception {
        return UUID.fromString(data(createGroup(bearer, title, members)).get("id").asString());
    }

    ResultActions send(String bearer, UUID conversationId, String body) throws Exception {
        return sendJson(bearer, conversationId, "{\"clientMessageId\": \"" + UUID.randomUUID() + "\", \"body\": \"" + body + "\"}");
    }

    ResultActions sendJson(String bearer, UUID conversationId, String json) throws Exception {
        return mockMvc.perform(post("/conversations/{id}/messages", conversationId).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    JsonNode sent(String bearer, UUID conversationId, String body) throws Exception {
        return data(send(bearer, conversationId, body));
    }

    ResultActions receipts(String bearer, UUID conversationId, long delivered, long read) throws Exception {
        return mockMvc.perform(put("/conversations/{id}/receipts", conversationId).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"deliveredSeq\": " + delivered + ", \"readSeq\": " + read + "}"));
    }

    ResultActions fetch(String bearer, String path, Object... variables) throws Exception {
        return mockMvc.perform(get(path, variables)
                .header(HttpHeaders.AUTHORIZATION, bearer));
    }

    ResultActions postTo(String bearer, String path, Object... variables) throws Exception {
        return mockMvc.perform(post(path, variables).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    ResultActions rename(String bearer, UUID conversationId, String title) throws Exception {
        return mockMvc.perform(patch("/conversations/{id}", conversationId).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"" + title + "\"}"));
    }

    ResultActions addMembers(String bearer, UUID conversationId, UUID... userIds) throws Exception {
        String ids = Arrays.stream(userIds).map(id -> "\"" + id + "\"").collect(Collectors.joining(","));
        return mockMvc.perform(post("/conversations/{id}/members", conversationId).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"userIds\": [" + ids + "]}"));
    }

    ResultActions removeMember(String bearer, UUID conversationId, UUID userId) throws Exception {
        return mockMvc.perform(delete("/conversations/{id}/members/{userId}", conversationId, userId)
                .header(HttpHeaders.AUTHORIZATION, bearer));
    }

    ResultActions deleteMessage(String bearer, UUID conversationId, UUID messageId) throws Exception {
        return mockMvc.perform(delete("/conversations/{id}/messages/{messageId}", conversationId, messageId)
                .header(HttpHeaders.AUTHORIZATION, bearer));
    }

    JsonNode data(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("data");
    }
}
