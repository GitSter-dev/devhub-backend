package com.application.devhub.notification;

import java.util.Collection;
import java.util.UUID;

public record NotificationsChanged(Collection<UUID> recipientIds) {
}
