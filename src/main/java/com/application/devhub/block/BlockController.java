package com.application.devhub.block;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.profile.PersonPage;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users/me/blocks")
@RequiredArgsConstructor
public class BlockController implements BlockApi {

    private final BlockService blockService;
    private final BlockListQuery blockListQuery;

    @Override
    @PutMapping("/{userId}")
    public ApiEnvelope<Void> block(JwtAuthenticationToken authentication, @PathVariable UUID userId) {
        blockService.block(userIdOf(authentication), userId);
        return ApiEnvelope.ok();
    }

    @Override
    @DeleteMapping("/{userId}")
    public ApiEnvelope<Void> unblock(JwtAuthenticationToken authentication, @PathVariable UUID userId) {
        blockService.unblock(userIdOf(authentication), userId);
        return ApiEnvelope.ok();
    }

    @Override
    @GetMapping
    public ApiEnvelope<PersonPage> blocked(JwtAuthenticationToken authentication,
                                           @RequestParam(required = false) String cursor) {
        return ApiEnvelope.ok(blockListQuery.blocked(userIdOf(authentication), cursor));
    }

    private static UUID userIdOf(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getName());
    }
}
