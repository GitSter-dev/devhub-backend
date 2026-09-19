package com.application.devhub.chat;

import com.application.devhub.chat.ChatViews.ConversationPage;
import com.application.devhub.chat.ChatViews.ConversationView;
import com.application.devhub.chat.ChatViews.MessagePage;
import com.application.devhub.chat.ChatViews.MessageView;
import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.ratelimit.RateLimitPolicy;
import com.application.devhub.ratelimit.RateLimitScope;
import com.application.devhub.ratelimit.RateLimited;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ChatController implements ChatApi {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final ReceiptService receiptService;
    private final ChatQueries chatQueries;

    @Override
    @GetMapping
    public ApiEnvelope<ConversationPage> conversations(JwtAuthenticationToken authentication,
                                                       @RequestParam(required = false) String cursor) {
        return ApiEnvelope.ok(chatQueries.inbox(userIdOf(authentication), cursor));
    }

    @Override
    @GetMapping("/requests")
    public ApiEnvelope<List<ConversationView>> requests(JwtAuthenticationToken authentication) {
        return ApiEnvelope.ok(chatQueries.requests(userIdOf(authentication)));
    }

    @Override
    @PostMapping("/direct")
    public ResponseEntity<ApiEnvelope<ConversationView>> openDirect(JwtAuthenticationToken authentication,
                                                                    @Valid @RequestBody OpenDirectRequest request) {
        UUID me = userIdOf(authentication);
        ConversationService.Opened opened = conversationService.openDirect(me, request.userId());
        return ResponseEntity.status(opened.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiEnvelope.ok(chatQueries.conversation(me, opened.conversationId())));
    }

    @Override
    @PostMapping("/groups")
    public ResponseEntity<ApiEnvelope<ConversationView>> createGroup(JwtAuthenticationToken authentication,
                                                                     @Valid @RequestBody CreateGroupRequest request) {
        UUID me = userIdOf(authentication);
        UUID groupId = conversationService.createGroup(me, request.title(), request.memberIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.ok(chatQueries.conversation(me, groupId)));
    }

    @Override
    @GetMapping("/{conversationId}")
    public ApiEnvelope<ConversationView> conversation(JwtAuthenticationToken authentication,
                                                      @PathVariable UUID conversationId) {
        return ApiEnvelope.ok(chatQueries.conversation(userIdOf(authentication), conversationId));
    }

    @Override
    @PatchMapping("/{conversationId}")
    public ApiEnvelope<ConversationView> rename(JwtAuthenticationToken authentication, @PathVariable UUID conversationId,
                                                @Valid @RequestBody RenameGroupRequest request) {
        UUID me = userIdOf(authentication);
        conversationService.rename(me, conversationId, request.title());
        return ApiEnvelope.ok(chatQueries.conversation(me, conversationId));
    }

    @Override
    @PostMapping("/{conversationId}/members")
    public ApiEnvelope<ConversationView> addMembers(JwtAuthenticationToken authentication,
                                                    @PathVariable UUID conversationId,
                                                    @Valid @RequestBody AddMembersRequest request) {
        UUID me = userIdOf(authentication);
        conversationService.addMembers(me, conversationId, request.userIds());
        return ApiEnvelope.ok(chatQueries.conversation(me, conversationId));
    }

    @Override
    @DeleteMapping("/{conversationId}/members/{userId}")
    public ApiEnvelope<Void> removeMember(JwtAuthenticationToken authentication, @PathVariable UUID conversationId,
                                          @PathVariable UUID userId) {
        conversationService.removeMember(userIdOf(authentication), conversationId, userId);
        return ApiEnvelope.ok();
    }

    @Override
    @PostMapping("/{conversationId}/accept")
    public ApiEnvelope<ConversationView> accept(JwtAuthenticationToken authentication, @PathVariable UUID conversationId) {
        UUID me = userIdOf(authentication);
        conversationService.accept(me, conversationId);
        return ApiEnvelope.ok(chatQueries.conversation(me, conversationId));
    }

    @Override
    @PostMapping("/{conversationId}/decline")
    public ApiEnvelope<Void> decline(JwtAuthenticationToken authentication, @PathVariable UUID conversationId) {
        conversationService.decline(userIdOf(authentication), conversationId);
        return ApiEnvelope.ok();
    }

    @Override
    @GetMapping("/{conversationId}/messages")
    public ApiEnvelope<MessagePage> messages(JwtAuthenticationToken authentication, @PathVariable UUID conversationId,
                                             @RequestParam(required = false) Long beforeSeq,
                                             @RequestParam(required = false) Long afterSeq,
                                             @RequestParam(defaultValue = "30") int limit) {
        return ApiEnvelope.ok(chatQueries.messages(userIdOf(authentication), conversationId, beforeSeq, afterSeq, limit));
    }

    @Override
    @PostMapping("/{conversationId}/messages")
    @RateLimited(value = RateLimitPolicy.MESSAGING, scope = RateLimitScope.USER)
    public ResponseEntity<ApiEnvelope<MessageView>> send(JwtAuthenticationToken authentication,
                                                         @PathVariable UUID conversationId,
                                                         @Valid @RequestBody SendMessageRequest request) {
        UUID messageId = messageService.send(userIdOf(authentication), conversationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.ok(chatQueries.message(messageId)));
    }

    @Override
    @DeleteMapping("/{conversationId}/messages/{messageId}")
    public ApiEnvelope<Void> deleteMessage(JwtAuthenticationToken authentication, @PathVariable UUID conversationId,
                                           @PathVariable UUID messageId) {
        messageService.delete(userIdOf(authentication), conversationId, messageId);
        return ApiEnvelope.ok();
    }

    @Override
    @PutMapping("/{conversationId}/receipts")
    public ApiEnvelope<ReceiptService.Watermarks> receipts(JwtAuthenticationToken authentication,
                                                           @PathVariable UUID conversationId,
                                                           @Valid @RequestBody ReceiptsRequest request) {
        return ApiEnvelope.ok(receiptService.advance(userIdOf(authentication), conversationId, request.deliveredSeq(),
                request.readSeq()));
    }

    private static UUID userIdOf(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getName());
    }
}
