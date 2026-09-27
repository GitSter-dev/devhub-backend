package com.application.devhub.post;

import com.application.devhub.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;

    @Column(length = 500)
    private String body;

    @Column(columnDefinition = "text")
    private String code;

    @Column(name = "code_language", length = 20)
    private String codeLanguage;

    @Column(name = "removed_at")
    private Instant removedAt;

    @Column(name = "hidden_at")
    private Instant hiddenAt;

    @Column(name = "parent_id", updatable = false)
    private UUID parentId;

    @Column(name = "root_id", updatable = false)
    private UUID rootId;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "community_id", updatable = false)
    private UUID communityId;

    @Column(name = "pinned_at")
    private Instant pinnedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "removal_scope", length = 20)
    private RemovalScope removalScope;

    private Post(UUID authorId, PostContent content, UUID parentId, UUID rootId, UUID communityId) {
        this.authorId = authorId;
        this.body = content.body();
        this.code = content.code();
        this.codeLanguage = content.codeLanguage();
        this.parentId = parentId;
        this.rootId = rootId;
        this.communityId = communityId;
    }

    public static Post original(UUID authorId, PostContent content, UUID communityId) {
        return new Post(authorId, content, null, null, communityId);
    }

    public static Post replyTo(Post parent, UUID authorId, PostContent content) {
        return new Post(authorId, content, parent.getId(), parent.isReply() ? parent.getRootId() : parent.getId(),
                parent.getCommunityId());
    }

    public boolean isReply() {
        return parentId != null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isAuthoredBy(UUID userId) {
        return authorId.equals(userId);
    }

    public void remove() {
        removeWithin(RemovalScope.PLATFORM);
    }

    public void removeFromCommunity() {
        if (removalScope != RemovalScope.PLATFORM) {
            removeWithin(RemovalScope.COMMUNITY);
        }
    }

    public boolean restoreToCommunity() {
        if (removalScope == RemovalScope.PLATFORM) {
            return false;
        }
        restore();
        return true;
    }

    public boolean isRemoved() {
        return removedAt != null;
    }

    public boolean isLiveTopLevel() {
        return !isReply() && !isDeleted() && !isRemoved();
    }

    public void pin() {
        if (pinnedAt == null) {
            this.pinnedAt = Instant.now();
        }
    }

    public void unpin() {
        this.pinnedAt = null;
    }

    private void removeWithin(RemovalScope scope) {
        this.removedAt = Instant.now();
        this.removalScope = scope;
        this.hiddenAt = null;
        this.pinnedAt = null;
    }

    public void hide() {
        if (hiddenAt == null && removedAt == null) {
            this.hiddenAt = Instant.now();
        }
    }

    public void restore() {
        this.removedAt = null;
        this.removalScope = null;
        this.hiddenAt = null;
    }

    public void delete() {
        if (deletedAt == null) {
            this.deletedAt = Instant.now();
            this.body = null;
            this.code = null;
            this.codeLanguage = null;
        }
    }
}
