package com.application.devhub.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Oldest app version each platform may still use, keyed by the {@code X-App-Platform} value.
 * A platform without an entry is never gated.
 */
@ConfigurationProperties("devhub.clients")
public record ClientVersionProperties(Map<String, String> minimumVersions) {

    public ClientVersionProperties {
        minimumVersions = minimumVersions == null ? Map.of() : Map.copyOf(minimumVersions);
        minimumVersions.forEach((platform, version) -> {
            if (ClientVersion.parse(version).isEmpty()) {
                throw new IllegalArgumentException(
                        "devhub.clients.minimum-versions." + platform + " is not MAJOR.MINOR.PATCH: " + version);
            }
        });
    }

    public Optional<ClientVersion> minimumFor(String platform) {
        return Optional.ofNullable(minimumVersions.get(platform.toLowerCase(Locale.ROOT)))
                .flatMap(ClientVersion::parse);
    }
}
