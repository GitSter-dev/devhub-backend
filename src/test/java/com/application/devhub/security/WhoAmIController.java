package com.application.devhub.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
class WhoAmIController {

    @GetMapping("/test/whoami")
    Map<String, Object> whoAmI(JwtAuthenticationToken authentication) {
        return Map.of(
                "subject", authentication.getName(),
                "authorities", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }
}
