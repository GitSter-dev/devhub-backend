package com.application.devhub.push;

import java.util.List;

public interface PushSender {

    List<PushResult> send(List<String> tokens, PushMessage message);
}
