package com.application.devhub.user;

import com.application.devhub.common.api.ApiEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final CurrentUserService currentUserService;
    private final UserSetupService userSetupService;

    @Override
    @GetMapping("/me")
    public ApiEnvelope<CurrentUserResponse> me(JwtAuthenticationToken authentication) {
        return ApiEnvelope.ok(currentUserService.get(UUID.fromString(authentication.getName())));
    }

    @Override
    @PutMapping("/me/setup-completion")
    public ApiEnvelope<CurrentUserResponse> completeSetup(JwtAuthenticationToken authentication) {
        return ApiEnvelope.ok(userSetupService.complete(UUID.fromString(authentication.getName())));
    }
}
