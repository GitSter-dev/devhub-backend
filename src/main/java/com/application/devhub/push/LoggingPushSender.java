package com.application.devhub.push;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnBooleanProperty(name = "devhub.push.enabled", havingValue = false, matchIfMissing = true)
public class LoggingPushSender implements PushSender {

    @Override
    public List<PushResult> send(List<String> tokens, PushMessage message) {
        log.info("Push disabled; would send \"{}\" to {} device(s)", message.title(), tokens.size());
        return tokens.stream().map(token -> new PushResult(token, PushOutcome.DELIVERED)).toList();
    }
}
