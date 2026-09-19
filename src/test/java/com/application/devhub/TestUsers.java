package com.application.devhub;

import com.application.devhub.user.User;
import com.application.devhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
@RequiredArgsConstructor
public class TestUsers {

    public static final String PASSWORD = "supersecret";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper;

    public UUID verified(String username) {
        return verified(username, username);
    }

    public UUID verified(String username, String displayName) {
        User user = new User(username, displayName, username + "@dev.io", passwordEncoder.encode(PASSWORD));
        user.markEmailVerified();
        return userRepository.save(user).getId();
    }

    public UUID unverified(String username) {
        return userRepository.save(new User(username, username, username + "@dev.io", passwordEncoder.encode(PASSWORD)))
                .getId();
    }

    public String bearer(String username) throws Exception {
        String response = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + jsonMapper.readTree(response).get("data").get("accessToken").asString();
    }
}
