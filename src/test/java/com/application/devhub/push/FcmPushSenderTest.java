package com.application.devhub.push;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FcmPushSenderTest {

    private static final PushMessage MESSAGE = new PushMessage("Title", "Body", Map.of("kind", "test"));

    private final FirebaseMessaging messaging = mock(FirebaseMessaging.class);
    private final FcmPushSender sender = new FcmPushSender(messaging);

    @Test
    void mapsEachResponseToTheOutcomeOfItsToken() throws Exception {
        BatchResponse batch = batchOf(
                delivered(),
                failedWith(MessagingErrorCode.UNREGISTERED),
                failedWith(MessagingErrorCode.INVALID_ARGUMENT),
                failedWith(MessagingErrorCode.UNAVAILABLE));
        when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batch);

        List<PushResult> results = sender.send(List.of("ok", "unregistered", "bad-request", "unavailable"), MESSAGE);

        assertThat(results).containsExactly(
                new PushResult("ok", PushOutcome.DELIVERED),
                new PushResult("unregistered", PushOutcome.TOKEN_INVALID),
                new PushResult("bad-request", PushOutcome.FAILED),
                new PushResult("unavailable", PushOutcome.FAILED));
    }

    @Test
    void anEmptyTokenListNeverReachesFcm() {
        assertThat(sender.send(List.of(), MESSAGE)).isEmpty();
    }

    private static BatchResponse batchOf(SendResponse... responses) {
        BatchResponse batch = mock(BatchResponse.class);
        when(batch.getResponses()).thenReturn(List.of(responses));
        return batch;
    }

    private static SendResponse delivered() {
        SendResponse response = mock(SendResponse.class);
        when(response.isSuccessful()).thenReturn(true);
        return response;
    }

    private static SendResponse failedWith(MessagingErrorCode code) {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(code);
        SendResponse response = mock(SendResponse.class);
        when(response.isSuccessful()).thenReturn(false);
        when(response.getException()).thenReturn(exception);
        return response;
    }
}
