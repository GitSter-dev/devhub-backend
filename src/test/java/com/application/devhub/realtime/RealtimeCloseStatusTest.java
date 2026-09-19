package com.application.devhub.realtime;

import com.application.devhub.session.RevocationReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimeCloseStatusTest {

    @Test
    void aReplacedSessionClosesWithTheReplacedCode() {
        assertThat(RealtimeCloseStatus.of(RevocationReason.REPLACED).getCode()).isEqualTo(4001);
        assertThat(RealtimeCloseStatus.of(RevocationReason.REPLACED).getReason()).isEqualTo("SESSION_REPLACED");
    }

    @ParameterizedTest
    @EnumSource(value = RevocationReason.class, names = "REPLACED", mode = EnumSource.Mode.EXCLUDE)
    void everyOtherReasonClosesAsEnded(RevocationReason reason) {
        assertThat(RealtimeCloseStatus.of(reason)).isEqualTo(RealtimeCloseStatus.SESSION_ENDED);
        assertThat(RealtimeCloseStatus.SESSION_ENDED.getCode()).isEqualTo(4002);
    }
}
