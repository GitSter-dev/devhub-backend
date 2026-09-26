package com.application.devhub.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientVersionTest {

    @Test
    void parsesReleaseVersionsAndIgnoresSuffixes() {
        assertThat(ClientVersion.parse("1.2.3")).contains(new ClientVersion(1, 2, 3));
        assertThat(ClientVersion.parse(" 1.2.3 ")).contains(new ClientVersion(1, 2, 3));
        assertThat(ClientVersion.parse("1.2.3-rc.1")).contains(new ClientVersion(1, 2, 3));
        assertThat(ClientVersion.parse("1.2.3+42")).contains(new ClientVersion(1, 2, 3));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "1", "1.2", "v1.2.3", "1.2.x", "1.2.3.4", "99999.0.0"})
    void rejectsAnythingElse(String value) {
        assertThat(ClientVersion.parse(value)).isEmpty();
    }

    @Test
    void comparesNumericallyNotLexically() {
        ClientVersion v1_9_0 = ClientVersion.parse("1.9.0").orElseThrow();
        ClientVersion v1_10_0 = ClientVersion.parse("1.10.0").orElseThrow();

        assertThat(v1_9_0.isOlderThan(v1_10_0)).isTrue();
        assertThat(v1_10_0.isOlderThan(v1_9_0)).isFalse();
        assertThat(v1_10_0.isOlderThan(v1_10_0)).isFalse();
        assertThat(ClientVersion.parse("2.0.0").orElseThrow().isOlderThan(v1_10_0)).isFalse();
    }

    @Test
    void minimumsAreLookedUpCaseInsensitivelyAndMustBeValid() {
        ClientVersionProperties properties = new ClientVersionProperties(Map.of("android", "1.0.1"));

        assertThat(properties.minimumFor("Android")).contains(new ClientVersion(1, 0, 1));
        assertThat(properties.minimumFor("ios")).isEmpty();
        assertThatThrownBy(() -> new ClientVersionProperties(Map.of("android", "latest")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimum-versions.android");
    }
}
