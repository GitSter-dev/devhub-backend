package com.application.devhub.security;

import com.application.devhub.IntegrationTest;
import com.application.devhub.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityChainIntegrationTest extends IntegrationTest {

    private static final String PROTECTED = "/test/whoami";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void missingTokenIsRejectedWithEnvelope() throws Exception {
        mockMvc.perform(get(PROTECTED))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void malformedTokenIsRejected() throws Exception {
        mockMvc.perform(get(PROTECTED).header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        Instant past = Instant.now().minus(Duration.ofHours(1));
        String token = sign(jwtEncoder, claims(jwtProperties.issuer(), past, past.plus(Duration.ofMinutes(5))));
        mockMvc.perform(get(PROTECTED).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedByAnotherKeyIsRejected() throws Exception {
        KeyPair foreign = newRsaKeyPair();
        JwtEncoder foreignEncoder = NimbusJwtEncoder
                .withKeyPair((RSAPublicKey) foreign.getPublic(), (RSAPrivateKey) foreign.getPrivate())
                .build();
        String token = sign(foreignEncoder, validClaims(jwtProperties.issuer()));
        mockMvc.perform(get(PROTECTED).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() throws Exception {
        String token = sign(jwtEncoder, validClaims("someone-else"));
        mockMvc.perform(get(PROTECTED).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void issuedTokenAuthenticatesWithSubjectAndRoles() throws Exception {
        AuthUser user = new AuthUser(UUID.randomUUID(), "ada", "{noop}x", Role.USER, true);
        mockMvc.perform(get(PROTECTED).header(HttpHeaders.AUTHORIZATION, bearer(tokenService.issueAccessToken(user, UUID.randomUUID()).value())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value(user.id().toString()))
                .andExpect(jsonPath("$.authorities", hasItem("ROLE_USER")));
    }

    @Test
    void authPathsArePublic() throws Exception {
        mockMvc.perform(get("/auth/anything"))
                .andExpect(status().is(not(401)));
    }

    @Test
    void authPathsIgnoreStaleBearerToken() throws Exception {
        Instant past = Instant.now().minus(Duration.ofHours(1));
        String expired = sign(jwtEncoder, claims(jwtProperties.issuer(), past, past.plus(Duration.ofMinutes(5))));
        mockMvc.perform(get("/auth/anything").header(HttpHeaders.AUTHORIZATION, bearer(expired)))
                .andExpect(status().is(not(401)));
    }

    @Test
    void noSessionIsCreated() throws Exception {
        AuthUser user = new AuthUser(UUID.randomUUID(), "ada", "{noop}x", Role.USER, true);
        mockMvc.perform(get(PROTECTED).header(HttpHeaders.AUTHORIZATION, bearer(tokenService.issueAccessToken(user, UUID.randomUUID()).value())))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(result -> {
                    if (result.getRequest().getSession(false) != null) {
                        throw new AssertionError("Expected no HTTP session");
                    }
                });
    }

    private static JwtClaimsSet validClaims(String issuer) {
        Instant now = Instant.now();
        return claims(issuer, now, now.plus(Duration.ofMinutes(5)));
    }

    private static JwtClaimsSet claims(String issuer, Instant issuedAt, Instant expiresAt) {
        return JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("roles", List.of("USER"))
                .build();
    }

    private static String sign(JwtEncoder encoder, JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
