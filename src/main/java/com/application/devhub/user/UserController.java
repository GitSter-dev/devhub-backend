package com.application.devhub.user;

import com.application.devhub.common.api.ApiEnvelope;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final CurrentUserService currentUserService;
    private final UserSetupService userSetupService;
    private final UsernameService usernameService;

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

    @Override
    @PutMapping("/me/username")
    public ApiEnvelope<CurrentUserResponse> changeUsername(JwtAuthenticationToken authentication,
                                                           @Valid @RequestBody ChangeUsernameRequest request) {
        return ApiEnvelope.ok(usernameService.change(UUID.fromString(authentication.getName()), request.username()));
    }

    @Override
    @GetMapping("/username-availability")
    public ApiEnvelope<UsernameAvailabilityResponse> usernameAvailability(JwtAuthenticationToken authentication,
                                                                          @RequestParam String username) {
        return ApiEnvelope.ok(usernameService.availability(UUID.fromString(authentication.getName()), username));
    }
}
