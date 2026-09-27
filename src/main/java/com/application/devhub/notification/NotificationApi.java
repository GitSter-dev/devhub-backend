package com.application.devhub.notification;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.openapi.ApiErrors;
import com.application.devhub.notification.NotificationViews.NotificationPage;
import com.application.devhub.notification.NotificationViews.UnseenCount;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static com.application.devhub.common.api.ErrorCode.BAD_REQUEST;

@Tag(name = "Notifications", description = "Likes, replies, new followers, posts from people you follow and message requests")
public interface NotificationApi {

    @Operation(summary = "List notifications",
            description = "Grouped activity, most recent first, 20 per page. Each row gathers everyone who did the same "
                    + "thing (\"Ada, Ken and 3 others liked your post\") until you see it; later activity starts a new "
                    + "row. actors holds the latest three, actorCount the total. targetId is what the latest actor "
                    + "touched: the reply, the post, the follower or the conversation. Chat messages never appear here. "
                    + "Types the calling app build can't show (per X-App-Version) are left out, and community is set "
                    + "for community notifications.")
    @ApiResponse(responseCode = "200", description = "A page of notifications", useReturnTypeSchema = true)
    @ApiErrors(BAD_REQUEST)
    ApiEnvelope<NotificationPage> notifications(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                                @Parameter(description = "nextCursor from the previous page") String cursor,
                                                @Parameter(hidden = true) HttpServletRequest request);

    @Operation(summary = "Count unseen notifications", description = "The number behind the bell badge.")
    @ApiResponse(responseCode = "200", description = "The unseen count", useReturnTypeSchema = true)
    ApiEnvelope<UnseenCount> unseenCount(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                         @Parameter(hidden = true) HttpServletRequest request);

    @Operation(summary = "Mark notifications seen",
            description = "Marks every notification last updated at or before until as seen and returns what is still "
                    + "unseen. New activity after that point starts fresh rows and pushes again.")
    @ApiResponse(responseCode = "200", description = "The remaining unseen count", useReturnTypeSchema = true)
    @ApiErrors(BAD_REQUEST)
    ApiEnvelope<UnseenCount> markSeen(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                      MarkSeenRequest request,
                                      @Parameter(hidden = true) HttpServletRequest httpRequest);
}
