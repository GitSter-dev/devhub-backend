package com.application.devhub.idempotency;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ReplayPolicyTest {

    @ParameterizedTest
    @ValueSource(ints = {200, 201, 202, 400, 401, 403, 404, 409, 422})
    void deterministicOutcomesAreReplayed(int status) {
        assertThat(ReplayPolicy.isReplayable(status)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {408, 429, 500, 502, 503, 504})
    void transientFailuresRunAgain(int status) {
        assertThat(ReplayPolicy.isReplayable(status)).isFalse();
    }
}
