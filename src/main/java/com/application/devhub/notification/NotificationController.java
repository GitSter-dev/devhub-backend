package com.application.devhub.notification;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.notification.NotificationViews.NotificationPage;
import com.application.devhub.notification.NotificationViews.UnseenCount;
import jakarta.validation.Valid;
import com.application.devhub.client.CallingApp;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationApi {

    private final NotificationQueries queries;
    private final NotificationWriter writer;

    @Override
    @GetMapping
    public ApiEnvelope<NotificationPage> notifications(JwtAuthenticationToken authentication,
                                                       @RequestParam(required = false) String cursor,
                                                       HttpServletRequest request) {
        return ApiEnvelope.ok(queries.page(userIdOf(authentication), shownTo(request), cursor));
    }

    @Override
    @GetMapping("/unseen-count")
    public ApiEnvelope<UnseenCount> unseenCount(JwtAuthenticationToken authentication, HttpServletRequest request) {
        return ApiEnvelope.ok(queries.unseenCount(userIdOf(authentication), shownTo(request)));
    }

    @Override
    @PostMapping("/seen")
    public ApiEnvelope<UnseenCount> markSeen(JwtAuthenticationToken authentication,
                                             @Valid @RequestBody MarkSeenRequest request,
                                             HttpServletRequest httpRequest) {
        UUID me = userIdOf(authentication);
        writer.markSeen(me, request.until());
        return ApiEnvelope.ok(queries.unseenCount(me, shownTo(httpRequest)));
    }

    private static Set<NotificationType> shownTo(HttpServletRequest request) {
        return NotificationType.shownTo(CallingApp.versionOf(request));
    }

    private static UUID userIdOf(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getName());
    }
}
