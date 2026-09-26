package com.application.devhub.client;

import com.application.devhub.IntegrationTest;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "devhub.clients.minimum-versions.android=1.2.0")
class ClientVersionFilterIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meters;

    @Test
    void anOutdatedAppIsToldToUpdateBeforeAuthenticationRuns() throws Exception {
        mockMvc.perform(login().header("X-App-Platform", "android").header("X-App-Version", "1.1.9"))
                .andExpect(status().isUpgradeRequired())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("APP_UPDATE_REQUIRED"))
                .andExpect(jsonPath("$.error.message").isNotEmpty());

        mockMvc.perform(get("/users/me").header("X-App-Platform", "android").header("X-App-Version", "1.0.0"))
                .andExpect(status().isUpgradeRequired());

        assertThat(meters.get("devhub.clients.outdated.rejections")
                .tags("platform", "android", "version", "1.1.9").counter().count()).isEqualTo(1);
    }

    @Test
    void theMinimumVersionAndNewerAreServed() throws Exception {
        for (String version : new String[] {"1.2.0", "1.2.1", "1.10.0", "2.0.0-rc.1"}) {
            mockMvc.perform(login().header("X-App-Platform", "android").header("X-App-Version", version))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
        }
    }

    @Test
    void clientsThatDontIdentifyThemselvesAreServed() throws Exception {
        mockMvc.perform(login()).andExpect(status().isUnauthorized());
        mockMvc.perform(login().header("X-App-Platform", "android")).andExpect(status().isUnauthorized());
        mockMvc.perform(login().header("X-App-Platform", "android").header("X-App-Version", "garbage"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(login().header("X-App-Platform", "ios").header("X-App-Version", "0.0.1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void operationalEndpointsAreNeverGated() throws Exception {
        mockMvc.perform(get("/actuator/health").header("X-App-Platform", "android").header("X-App-Version", "0.1.0"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs").header("X-App-Platform", "android").header("X-App-Version", "0.1.0"))
                .andExpect(status().isOk());
    }

    @Test
    void theHeadersAndTheUpgradeResponseAreDocumented() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/auth/login'].post.parameters[?(@.name == 'X-App-Platform')].in")
                        .value("header"))
                .andExpect(jsonPath("$.paths['/auth/login'].post.parameters[?(@.name == 'X-App-Version')].in")
                        .value("header"))
                .andExpect(jsonPath("$.paths['/auth/login'].post.responses['426'].content['application/json']"
                        + ".examples.APP_UPDATE_REQUIRED").exists());
    }

    private static MockHttpServletRequestBuilder login() {
        return post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"nobody\",\"password\":\"wrong-password\"}");
    }
}
