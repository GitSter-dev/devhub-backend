package com.application.devhub.account;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.session.TokenPair;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AccountController implements AccountApi {

    private final AccountService accountService;

    @Override
    @PostMapping("/users/me/deletion")
    public ApiEnvelope<Void> requestDeletion(JwtAuthenticationToken authentication,
                                             @Valid @RequestBody DeleteAccountRequest request) {
        accountService.requestDeletion(UUID.fromString(authentication.getName()), request.password());
        return ApiEnvelope.ok();
    }

    @Override
    @PostMapping("/auth/restore")
    public ApiEnvelope<TokenPair> restore(@Valid @RequestBody RestoreAccountRequest request) {
        return ApiEnvelope.ok(accountService.restore(request.identifier(), request.password()));
    }
}
