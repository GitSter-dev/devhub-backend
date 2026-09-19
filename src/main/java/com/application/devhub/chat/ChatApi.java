package com.application.devhub.chat;

import com.application.devhub.chat.ChatViews.ConversationPage;
import com.application.devhub.chat.ChatViews.ConversationView;
import com.application.devhub.chat.ChatViews.MessagePage;
import com.application.devhub.chat.ChatViews.MessageView;
import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.openapi.ApiErrors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static com.application.devhub.common.api.ErrorCode.BAD_REQUEST;
import static com.application.devhub.common.api.ErrorCode.CANNOT_MESSAGE_YOURSELF;
import static com.application.devhub.common.api.ErrorCode.CONVERSATION_REQUEST_PENDING;
import static com.application.devhub.common.api.ErrorCode.FORBIDDEN;
import static com.application.devhub.common.api.ErrorCode.GROUP_TOO_LARGE;
import static com.application.devhub.common.api.ErrorCode.IDEMPOTENCY_KEY_REUSED;
import static com.application.devhub.common.api.ErrorCode.NOT_FOUND;
import static com.application.devhub.common.api.ErrorCode.NOT_GROUP_OWNER;

@Tag(name = "Chat", description = "Direct messages, group chats, message requests and read receipts")
public interface ChatApi {

    @Operation(summary = "List conversations",
            description = "Active conversations that have at least one message, most recent activity first, 20 per page. "
                    + "Each carries its members, last message, unread count and othersDeliveredSeq/othersReadSeq: the "
                    + "lowest watermark among the other members, which drives the delivered/read ticks on your messages.")
    @ApiResponse(responseCode = "200", description = "A page of conversations", useReturnTypeSchema = true)
    @ApiErrors(BAD_REQUEST)
    ApiEnvelope<ConversationPage> conversations(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                                @Parameter(description = "nextCursor from the previous page") String cursor);

    @Operation(summary = "List message requests",
            description = "DMs from people you don't follow, waiting for you to accept or decline. Senders don't see "
                    + "your receipts and you get no push until you accept.")
    @ApiResponse(responseCode = "200", description = "Pending requests", useReturnTypeSchema = true)
    ApiEnvelope<List<ConversationView>> requests(@Parameter(hidden = true) JwtAuthenticationToken authentication);

    @Operation(summary = "Open a direct conversation",
            description = "Returns the existing DM with this developer, or starts one (201). A DM to someone who doesn't "
                    + "follow you lands in their message requests.")
    @ApiResponse(responseCode = "200", description = "The conversation", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, CANNOT_MESSAGE_YOURSELF})
    ResponseEntity<ApiEnvelope<ConversationView>> openDirect(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                                             OpenDirectRequest request);

    @Operation(summary = "Create a group chat",
            description = "You become the owner. Up to 50 members including you; everyone must be a verified developer.")
    @ApiResponse(responseCode = "201", description = "The new group", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, GROUP_TOO_LARGE})
    ResponseEntity<ApiEnvelope<ConversationView>> createGroup(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                                              CreateGroupRequest request);

    @Operation(summary = "Get a conversation",
            description = "Members with roles and receipt watermarks. Watermarks of members who haven't accepted a "
                    + "request are hidden from everyone else.")
    @ApiResponse(responseCode = "200", description = "The conversation", useReturnTypeSchema = true)
    @ApiErrors(NOT_FOUND)
    ApiEnvelope<ConversationView> conversation(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                               @Parameter(description = "The conversation") UUID conversationId);

    @Operation(summary = "Rename a group", description = "Owner only. Posts a system line to the group.")
    @ApiResponse(responseCode = "200", description = "The renamed group", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, NOT_GROUP_OWNER, BAD_REQUEST})
    ApiEnvelope<ConversationView> rename(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                         @Parameter(description = "The group") UUID conversationId,
                                         RenameGroupRequest request);

    @Operation(summary = "Add members to a group",
            description = "Owner only. People who left can be added back. New members start with the history already "
                    + "marked read. GROUP_TOO_LARGE past 50 members.")
    @ApiResponse(responseCode = "200", description = "The group with its new members", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, NOT_GROUP_OWNER, GROUP_TOO_LARGE, BAD_REQUEST})
    ApiEnvelope<ConversationView> addMembers(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                             @Parameter(description = "The group") UUID conversationId,
                                             AddMembersRequest request);

    @Operation(summary = "Remove a member or leave a group",
            description = "Pass your own id to leave; if the owner leaves, the longest-standing member becomes owner. "
                    + "Removing someone else is owner only.")
    @ApiResponse(responseCode = "200", description = "Done", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, NOT_GROUP_OWNER, BAD_REQUEST})
    ApiEnvelope<Void> removeMember(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                   @Parameter(description = "The group") UUID conversationId,
                                   @Parameter(description = "The member to remove, or yourself to leave") UUID userId);

    @Operation(summary = "Accept a message request", description = "Moves the DM to your conversations. Accepting twice is harmless.")
    @ApiResponse(responseCode = "200", description = "Accepted", useReturnTypeSchema = true)
    @ApiErrors(NOT_FOUND)
    ApiEnvelope<ConversationView> accept(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                         @Parameter(description = "The requested conversation") UUID conversationId);

    @Operation(summary = "Decline a message request",
            description = "Hides the DM from you. The sender isn't told. Opening a DM with them yourself restores it.")
    @ApiResponse(responseCode = "200", description = "Declined", useReturnTypeSchema = true)
    @ApiErrors(NOT_FOUND)
    ApiEnvelope<Void> decline(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                              @Parameter(description = "The requested conversation") UUID conversationId);

    @Operation(summary = "List messages",
            description = "Always oldest first within the page. Without parameters: the newest messages. beforeSeq loads "
                    + "older history; afterSeq fills a gap after the last seq you have. hasMore says whether more exist "
                    + "in the direction you asked.")
    @ApiResponse(responseCode = "200", description = "A page of messages", useReturnTypeSchema = true)
    @ApiErrors(NOT_FOUND)
    ApiEnvelope<MessagePage> messages(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                      @Parameter(description = "The conversation") UUID conversationId,
                                      @Parameter(description = "Load messages older than this seq") Long beforeSeq,
                                      @Parameter(description = "Load messages newer than this seq") Long afterSeq,
                                      @Parameter(description = "Page size, 1 to 100", example = "30") int limit);

    @Operation(summary = "Send a message",
            description = "Text, a code block or both, optionally replying to a message in the same conversation. "
                    + "Resending the same clientMessageId returns the original message instead of a duplicate. The 201 "
                    + "response is the sent tick. CONVERSATION_REQUEST_PENDING until you accept a request.")
    @ApiResponse(responseCode = "201", description = "The stored message", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, CONVERSATION_REQUEST_PENDING, IDEMPOTENCY_KEY_REUSED})
    ResponseEntity<ApiEnvelope<MessageView>> send(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                                  @Parameter(description = "The conversation") UUID conversationId,
                                                  SendMessageRequest request);

    @Operation(summary = "Delete a message for everyone",
            description = "Your own messages only. It stays in place as a deleted placeholder.")
    @ApiResponse(responseCode = "200", description = "Deleted", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, FORBIDDEN})
    ApiEnvelope<Void> deleteMessage(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                    @Parameter(description = "The conversation") UUID conversationId,
                                    @Parameter(description = "The message") UUID messageId);

    @Operation(summary = "Report delivered and read receipts",
            description = "Watermarks only move forward and are capped at the newest message; reading implies delivery. "
                    + "Returns your watermarks after the update.")
    @ApiResponse(responseCode = "200", description = "Your watermarks", useReturnTypeSchema = true)
    @ApiErrors(NOT_FOUND)
    ApiEnvelope<ReceiptService.Watermarks> receipts(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                                    @Parameter(description = "The conversation") UUID conversationId,
                                                    ReceiptsRequest request);
}
