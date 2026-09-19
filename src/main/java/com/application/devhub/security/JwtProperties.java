package com.application.devhub.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;

@ConfigurationProperties("devhub.jwt")
public record JwtProperties(
        String issuer,
        Duration accessTokenTtl,
        RSAPublicKey publicKey,
        RSAPrivateKey privateKey) {
}
