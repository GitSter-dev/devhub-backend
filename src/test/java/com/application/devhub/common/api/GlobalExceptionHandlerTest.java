package com.application.devhub.common.api;

import com.application.devhub.client.ClientVersionFilter;
import com.application.devhub.idempotency.IdempotencyFilter;
import com.application.devhub.ratelimit.RateLimiter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.ProbeController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {IdempotencyFilter.class, ClientVersionFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandlerTest.ProbeController.class, GlobalExceptionHandler.class, ErrorMessageResolver.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RateLimiter rateLimiter;

    @Test
    void successIsWrappedWithoutError() throws Exception {
        mockMvc.perform(get("/probe/ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("devhub"))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void apiExceptionUsesItsErrorCodeStatus() throws Exception {
        mockMvc.perform(get("/probe/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message").value("The request conflicts with existing data"));
    }

    @Test
    void invalidBodyReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/probe/validate").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.fieldErrors.name").exists());
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mockMvc.perform(post("/probe/validate").contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void unsupportedMethodIsMethodNotAllowed() throws Exception {
        mockMvc.perform(delete("/probe/ok"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void unexpectedExceptionHidesInternals() throws Exception {
        mockMvc.perform(get("/probe/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("An unexpected error occurred"));
    }

    record ProbeRequest(@NotBlank String name) {
    }

    @RestController
    static class ProbeController {

        @GetMapping("/probe/ok")
        ApiEnvelope<Map<String, String>> ok() {
            return ApiEnvelope.ok(Map.of("name", "devhub"));
        }

        @GetMapping("/probe/conflict")
        ApiEnvelope<Void> conflict() {
            throw ApiException.conflict();
        }

        @PostMapping("/probe/validate")
        ApiEnvelope<Void> validate(@Valid @RequestBody ProbeRequest request) {
            return ApiEnvelope.ok();
        }

        @GetMapping("/probe/boom")
        ApiEnvelope<Void> boom() {
            throw new IllegalStateException("secret internal detail");
        }
    }
}
