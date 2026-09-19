package com.application.devhub.push;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnBooleanProperty(name = "devhub.push.enabled")
public class FcmPushSender implements PushSender {

    private static final String ANDROID_CHANNEL = "activity";

    private final FirebaseMessaging messaging;

    @Override
    public List<PushResult> send(List<String> tokens, PushMessage message) {
        if (tokens.isEmpty()) {
            return List.of();
        }
        MulticastMessage multicast = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder().setTitle(message.title()).setBody(message.body()).build())
                .putAllData(message.data())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(androidNotification(message))
                        .build())
                .build();
        try {
            return resultsOf(tokens, messaging.sendEachForMulticast(multicast));
        } catch (FirebaseMessagingException e) {
            throw new PushDeliveryException("FCM rejected the batch", e);
        }
    }

    private static AndroidNotification androidNotification(PushMessage message) {
        AndroidNotification.Builder builder = AndroidNotification.builder().setChannelId(ANDROID_CHANNEL);
        if (message.group() != null) {
            builder.setTag(message.group());
        }
        return builder.build();
    }

    private List<PushResult> resultsOf(List<String> tokens, BatchResponse batch) {
        List<PushResult> results = new ArrayList<>();
        for (int index = 0; index < tokens.size(); index++) {
            results.add(new PushResult(tokens.get(index), outcomeOf(batch.getResponses().get(index))));
        }
        return results;
    }

    private PushOutcome outcomeOf(SendResponse response) {
        if (response.isSuccessful()) {
            return PushOutcome.DELIVERED;
        }
        MessagingErrorCode code = response.getException().getMessagingErrorCode();
        return code == MessagingErrorCode.UNREGISTERED ? PushOutcome.TOKEN_INVALID : PushOutcome.FAILED;
    }
}
